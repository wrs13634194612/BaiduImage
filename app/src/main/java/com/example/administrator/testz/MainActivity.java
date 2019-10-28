package com.example.administrator.testz;

import android.os.Bundle;


import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;


import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;


import com.example.administrator.testz.BuildConfig;
import com.example.administrator.testz.R;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.example.administrator.testz.util.BitmapToByte;
import com.example.administrator.testz.util.BitmapUtil;
import com.example.administrator.testz.util.Classify;
import com.example.administrator.testz.util.Constants;
import com.example.administrator.testz.util.DataUtil;
import com.example.administrator.testz.util.LineProgress;
import com.example.administrator.testz.util.PhotoProcess;
import com.example.administrator.testz.util.ToastUtil;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends AppCompatActivity {
    private MainModel mViewModel;
    private Spinner spinnerCountry;
    private ImageView mImage, mChooseLocalImage, mCapture;
    private View line;
    private String imagePath = null;
    private Bitmap mBitmap = null;//待处理的位图，默认为空
    private JSONObject json;
    private static final int ERROR = 5;

    private LineProgress lineHandler = new LineProgress();
    private BitmapUtil mBu = new BitmapUtil();
    private DataUtil mDu = new DataUtil();
    private PhotoProcess photoProcess = new PhotoProcess();
    private BitmapToByte bitmapToByte = new BitmapToByte();
    private Classify classify = new Classify();

    private  String countries[] = {"通用物体识别","菜品识别","车型识别","logo商标识别"
            ,"动物识别","植物识别","图像主体检测","地标识别"
            ,"花卉识别","食材识别","红酒识别","货币识别"};
    private int style = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        mImage = findViewById(R.id.initImage_5);
        spinnerCountry = findViewById(R.id.spinnerCountry);
        mImage.setImageResource(R.drawable.wallpaper);
        line = findViewById(R.id.progress_line_5);
        mChooseLocalImage = findViewById(R.id.local_image_5);
        mChooseLocalImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPhoto();
            }
        });

        mCapture = findViewById(R.id.capture_image_5);
        mCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto();
            }
        });

        final ImageView mUpload = findViewById(R.id.upload_5);
        mUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processPhoto();
            }
        });


        final ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,countries);
        spinnerCountry.setAdapter(adapter);
        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //String data = (String)spinnerCountry.getItemAtPosition(i);//从spinner中获取被选择的数据
                style = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel = ViewModelProviders.of(this).get(MainModel.class);
    }

    private void getPhoto() {
        Intent chooseImage = new Intent(Intent.ACTION_PICK);
        chooseImage.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        if (chooseImage.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooseImage, Constants.ACTION_CHOOSE_IMAGE);
        } else {
            ToastUtil.showToast(getApplicationContext(), "访问图库失败！");
        }
    }

    private void capturePhoto() {
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "photo_invo.jpg");
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (capture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(capture, Constants.REQUEST_CODE_TAKE_PICTURE);
        } else {
            ToastUtil.showToast(getApplicationContext(), "没有读取到拍摄图片！");
        }
    }

    private void processPhoto() {
        if (mBitmap == null) {
            ToastUtil.showToast(getApplicationContext(), "没有位图！");
            return;
        } else {
            lineHandler.lineProgress(mImage, line);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    classify.detected();
                    try {
                        int what = services();
                        Message message = Message.obtain();
                        message.what = what;
                        message.obj = json;
                        Log.e("TAG","res_five:"+json);
                        mHandler.sendMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Message message = Message.obtain();
                        message.what = ERROR;
                        message.obj = e;
                        mHandler.sendMessage(message);
                    }
                }
            }).start();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            lineHandler.getLineAnimation().cancel();
            line.setVisibility(View.INVISIBLE);
            try {
                JSONObject jsonObject5 = (JSONObject) msg.obj;
                JSONArray jsonArray5 = new JSONArray(jsonObject5.optString("result"));
                String name5 = jsonArray5.optJSONObject(0).optString("baike_info");
                String score5 = jsonArray5.optJSONObject(0).optString("score");
                String[] mitems5 = {"名称：" + name5, "可能性：" + score5};
                AlertDialog.Builder alertDialog5 = new AlertDialog.Builder(MainActivity.this);
                alertDialog5.setTitle("通用识别报告").setItems(mitems5, null).create().show();
            } catch (JSONException e) {
                e.printStackTrace();
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("解析错误，请看log日志").setMessage("图片无法解析").create().show();
            }
        }
    };

    // advancedGeneral
    protected int services() {
        classify.detected();
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("baike_num", "5");


    switch (style){
        case 0:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().advancedGeneral(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().advancedGeneral(bitmapToByte.getArrays(), options);
            }
            break;
        case 1:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().dishDetect(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().dishDetect(bitmapToByte.getArrays(), options);
            }
            break;
        case 2:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().carDetect(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().carDetect(bitmapToByte.getArrays(), options);
            }
            break;
        case 3:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().logoSearch(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().logoSearch(bitmapToByte.getArrays(), options);
            }
            break;

        case 4:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().animalDetect(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().animalDetect(bitmapToByte.getArrays(), options);
            }
            break;
        case 5:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().plantDetect(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().plantDetect(bitmapToByte.getArrays(), options);
            }
            break;
        case 6:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().objectDetect(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().objectDetect(bitmapToByte.getArrays(), options);
            }
            break;
        case 7:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().landmark(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().landmark(bitmapToByte.getArrays(), options);
            }
            break;
        case 8:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().flower(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().flower(bitmapToByte.getArrays(), options);
            }
            break;
        case 9:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().ingredient(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().ingredient(bitmapToByte.getArrays(), options);
            }
            break;
        case 10:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().redwine(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().redwine(bitmapToByte.getArrays(), options);
            }
            break;
        case 11:
            if (imagePath == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.id.initImage_1);
                json = classify.getClient().currency(bitmapToByte.getArrays(), options);
            } else {
                json = classify.getClient().currency(bitmapToByte.getArrays(), options);

            }
            break;

    }

        return 5;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (requestCode == Constants.ACTION_CHOOSE_IMAGE) {
                    if (data == null) {
                        ToastUtil.showToast(getApplicationContext(), "没有选中内容！");
                        return;
                    } else {
                        Uri uri = data.getData();
                        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                        cursor.moveToNext();
                        imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                        cursor.close();
                    }
                }
                break;
            case 2:
                if (requestCode == Constants.REQUEST_CODE_TAKE_PICTURE) {
                    if (data == null) {
                        ToastUtil.showToast(getApplicationContext(), "没有拍摄内容！");
                        return;
                    } else {
                        if (data.hasExtra("data")) {
                            mDu.setData(data);
                            mBitmap = (Bitmap) mDu.getData().getExtras().get("data");
                        } else {
                            ToastUtil.showToast(getApplicationContext(), "没有捕捉到二次图像");
                        }
                    }
                }
                break;
        }
        if (requestCode == Constants.ACTION_CHOOSE_IMAGE || requestCode == Constants.REQUEST_CODE_TAKE_PICTURE) {
            if (requestCode == Constants.ACTION_CHOOSE_IMAGE) {
                photoProcess.handle_photo(mBitmap, imagePath);
                mBitmap = photoProcess.getBitmap();
                bitmapToByte.bitmapToByte(mBitmap);
                mBitmap = bitmapToByte.getBitmap();
            } else if (requestCode == Constants.REQUEST_CODE_TAKE_PICTURE) {
                bitmapToByte.bitmapToByte(mBitmap);
                mBitmap = bitmapToByte.getBitmap();
            }
            mImage.setImageBitmap(mBitmap);
            mBu.setBitmap(mBitmap);
        }
    }


}

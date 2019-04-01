package com.liu.allcontacts;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity {
    public static final int END =0;
    Button start,look;
    AlertDialog dialog;
    Handler mHandler;
    File file;
    String fileName;
    private String[] title = {"编号","姓名","手机号"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = findViewById(R.id.start);
        look = findViewById(R.id.find);
        dialog = new AlertDialog.Builder(this).create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        View dialogContent = LayoutInflater.from(this).inflate(R.layout.loading_dialog,null);
        dialog.setView(dialogContent);
        dialog.setCanceledOnTouchOutside(false);
        initListener();
        initHandler();
    }

    private void initHandler(){
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case END:
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "查找结束", Toast.LENGTH_SHORT).show();
                        break;
                }
                return false;
            }
        });
    }
    private void initListener(){
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               requestPermission(MainActivity.this);
            }
        });
        look.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });
    }

    public void startThread(){
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
               ArrayList<ContactInfo> list =  getAllContacts(MainActivity.this);
                exportExcel(list);
                mHandler.sendEmptyMessage(END);
            }
        }).start();
    }
    /**
     * 查找联系人列表
     */
    public static ArrayList<ContactInfo> getAllContacts(Context context) {

        ArrayList<ContactInfo> list = new ArrayList<>();
        // 获取解析者
        ContentResolver resolver = context.getContentResolver();
        // 访问地址
        Uri raw_contacts = Uri.parse("content://com.android.contacts/raw_contacts");
        Uri data = Uri.parse("content://com.android.contacts/data");
        // 查询语句
        // select contact_id from raw_contacts;//1 2 3 4
        // select mimetype,data1 from view_data where raw_contact_id=3;
        // Cursor cursor=resolver.query(访问地址, 返回字段 null代表全部, where 语句, 参数, 排序)
        Cursor cursor = resolver.query(raw_contacts, new String[] { "contact_id" }, null, null, null);

        while (cursor.moveToNext()) {
            // getColumnIndex根据名称查列号
            String id = cursor.getString(cursor.getColumnIndex("contact_id"));
            // 创建实例
            String name = "";
            String phone = "";
            Cursor item = resolver.query(data, new String[] { "mimetype", "data1" }, "raw_contact_id=?", new String[] { id }, null);

            while (item.moveToNext()) {
                String mimetype = item.getString(item.getColumnIndex("mimetype"));
                String data1 = item.getString(item.getColumnIndex("data1"));
                if ("vnd.android.cursor.item/name".equals(mimetype)) {
                    name = data1;
                } else if ("vnd.android.cursor.item/phone_v2".equals(mimetype)) {
                    // 有的手机号中间会带有空格
                    phone = data1.replace(" ","");
                }
            }
            ContactInfo info = new ContactInfo(id,name,phone);
            item.close();
            // 添加集合
            list.add(info);
        }

        cursor.close();
        return list;
    }


    /**
     * 导出excel
     */
    public void exportExcel(ArrayList<ContactInfo> list) {
        file = new File(getSDPath() + "/Record");
        makeDir(file);
        ExcelUtils.initExcel(file.toString() + "/通讯录.xls", title);
        fileName = getSDPath() + "/Record/通讯录.xls";
        ExcelUtils.writeObjListToExcel(list, fileName, this);
    }
    private  String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        String dir = sdDir.toString();
        return dir;
    }
    public  void makeDir(File dir) {
        if (!dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        dir.mkdir();
    }
    /**
     * 请求授权
     */
    private void requestPermission(Context context){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){ //表示未授权时
            //进行授权
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},1);
        }else{
            //调用的方法
            startThread();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){ //同意权限申请
                    startThread();
                }else { //拒绝权限申请
                    Toast.makeText(this,"权限被拒绝了",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    public void openFile(){

        File file = new File(fileName);
//获取父目录
        File parentFlie = new File(file.getParent());
        Intent intent = new Intent(Intent.ACTION_VIEW);
//判断是
    Uri photoUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                parentFlie);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivity(intent);
    }
}

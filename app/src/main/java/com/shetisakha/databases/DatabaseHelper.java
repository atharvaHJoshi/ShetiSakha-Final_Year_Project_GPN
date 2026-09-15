package com.shetisakha.databases;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "robopath";

    String DB_Path = null;
    private static String DB_Name = "mempath.db";
    private SQLiteDatabase myDatabase;
    private final Context mycontext;

    public DatabaseHelper(Context context) {
        super(context, DB_Name, null, 1);
        this.mycontext = context;
        this.DB_Path = "/data/data/" + context.getPackageName() + "/" + "databases/";
        Log.e("Path 1", DB_Path);
    }

    public void createDatabase() throws IOException {
        boolean dbExsist = checkDatabase();
        if(dbExsist){

        }else {
            this.getReadableDatabase();
            this.close();
            try{
                copyDatabase();
            }catch (IOException e){
                throw new Error("Error copeing database");
            }
        }
    }

    private boolean checkDatabase(){
        SQLiteDatabase chckDB = null;
        try{
            String myPath = DB_Path + DB_Name;
            chckDB = SQLiteDatabase.openDatabase(myPath,null,SQLiteDatabase.OPEN_READONLY);
        }catch (SQLException e){
        }
        if(chckDB != null){
            chckDB.close();
        }
        return chckDB != null ? true:false;
    }

    private void copyDatabase() throws IOException{
        InputStream myInput = mycontext.getAssets().open(DB_Name);
        String outFilename = DB_Path + DB_Name;
        OutputStream myOutput = new FileOutputStream(outFilename);
        byte[] buffer = new byte[1000];
        int length;
        while ((length = myInput.read(buffer)) > 0 ){
            myOutput.write(buffer,0,length);
        }
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public void openDatabase() throws SQLException {
        String myPath = DB_Path + DB_Name;
        myDatabase = SQLiteDatabase.openDatabase(myPath,null,SQLiteDatabase.OPEN_READONLY);
    }


    @Override
    public synchronized void close(){
        if(myDatabase != null){
            myDatabase.close();
        }
        super.close();
    }
    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        //onCreate(db);
        if(newVersion > oldVersion){
            try{
                copyDatabase();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public boolean InsertPath(String direction, String runtime)
    {
        try
        {
            SQLiteDatabase db = this.getWritableDatabase();
            String sst = "insert into "+ TABLE_NAME + " (direction,runtime) values ('" + direction + "','" + runtime + "')";
            db.execSQL(sst);
            return true;
        }catch (SQLException e)
        {
            return false;
        }
    }

    public boolean DeleteScheduler()
    {
        try
        {
            SQLiteDatabase db = this.getWritableDatabase();
            String sst = "delete from "+ TABLE_NAME;
            db.execSQL(sst);
            return true;
        }catch (SQLException e)
        {
            return false;
        }
    }

    public Cursor GetPath(String condation){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+ TABLE_NAME + " " + condation,null);
        return res;
    }

    public boolean CreateTable()
    {
        try
        {
            if(CheckTable() == false){
                SQLiteDatabase db = this.getWritableDatabase();
                String sst = "CREATE TABLE robopath ( 'ID' INTEGER PRIMARY KEY AUTOINCREMENT, 'direction' TEXT, 'runtime' TEXT )";
                db.execSQL(sst);
            }
            return true;
        }catch (SQLException e)
        {
            return false;
        }
    }

    public boolean CheckTable(){
        Cursor res = null;
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            res = db.rawQuery("select * from "+ TABLE_NAME,null);
        }catch (SQLException e){
        }
        return res != null ? true:false;
    }
}

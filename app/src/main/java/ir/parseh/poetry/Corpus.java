package ir.parseh.poetry;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;

final class Corpus {
    private final SQLiteDatabase db;
    Corpus(Context context) throws IOException {
        File target = context.getDatabasePath("poetry.db");
        target.getParentFile().mkdirs();
        if (!target.exists()) {
            File temp = new File(target + ".tmp");
            try (InputStream in = context.getAssets().open("poetry.db"); OutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[65536]; int n;
                while ((n=in.read(buf))!=-1) out.write(buf,0,n);
            }
            if (!temp.renameTo(target)) throw new IOException("Database installation failed");
        }
        db = SQLiteDatabase.openDatabase(target.getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }
    List<String[]> query(String sql, String... args) {
        List<String[]> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql,args)) {
            while(c.moveToNext()) { String[] row = new String[c.getColumnCount()]; for(int i=0;i<row.length;i++) row[i]=c.getString(i); rows.add(row); }
        }
        return rows;
    }
    static String normalize(String s) {
        return s.replace('ي','ی').replace('ى','ی').replace('ك','ک').replace('\u200c',' ').replace("ـ", "").replaceAll("[\u064b-\u065f\u0670]", "").replaceAll("\\s+"," ").trim();
    }
    static String match(String s) {
        StringBuilder b = new StringBuilder();
        for(String word:normalize(s).split(" ")) if(!word.isEmpty()) { if(b.length()>0)b.append(" AND "); b.append('"').append(word.replace("\"","\"\"")).append('"'); }
        return b.toString();
    }
}

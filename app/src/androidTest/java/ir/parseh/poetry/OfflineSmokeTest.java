package ir.parseh.poetry;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OfflineSmokeTest {
    @Test public void fullCorpusOpensAndSearchesOffline() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        Corpus corpus=new Corpus(context);
        JSONObject version;
        try(InputStream in=context.getAssets().open("data-version.json")){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            version=new JSONObject(out.toString("UTF-8"));
        }
        assertEquals(version.getInt("poems"),Integer.parseInt(corpus.query("SELECT count(*) FROM poems").get(0)[0]));
        assertEquals(version.getInt("poets"),Integer.parseInt(corpus.query("SELECT count(*) FROM poets").get(0)[0]));
        assertFalse(corpus.query("SELECT docid FROM search WHERE search MATCH ? LIMIT 1",Corpus.match("عشق آسان")).isEmpty());
        assertFalse(corpus.query("SELECT body FROM poems WHERE id=?","2130").get(0)[0].isEmpty());
    }
    @Test public void longPersianPoemProducesReadableMultipleImages() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        Typeface font=Typeface.createFromAsset(context.getAssets(),"Vazirmatn.ttf");
        StringBuilder body=new StringBuilder();for(int i=0;i<100;i++)body.append("که عشق آسان نمود اول ولی افتاد مشکل‌ها\n");
        Intent share=Cards.create(context,font,body.toString(),"حافظ · غزل اول","https://ganjoor.net/hafez/ghazal/sh1",0);
        assertEquals(Intent.ACTION_SEND_MULTIPLE,share.getAction());
        ArrayList<Uri> pages=share.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        assertTrue(pages.size()>1);
        for(Uri uri:pages)try(InputStream in=context.getContentResolver().openInputStream(uri)){
            Bitmap image=BitmapFactory.decodeStream(in);assertNotNull(image);assertEquals(1080,image.getWidth());assertEquals(1440,image.getHeight());image.recycle();
        }
        assertEquals(pages.size(),share.getClipData().getItemCount());
    }
}

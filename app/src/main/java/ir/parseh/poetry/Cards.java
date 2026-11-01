package ir.parseh.poetry;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.text.*;
import java.io.*;
import java.util.*;

final class Cards {
    // Fixed-size pages bound peak bitmap memory, including for book-length poems.
    static Intent create(Context ctx,Typeface font,String body,String title,String source,int theme) throws IOException {
        int[] bg={0xfff7f4ed,0xff235d50,0xff172522};int[] fg={0xff243b35,0xfff7f4ed,0xffeee6d3};
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(font);paint.setTextSize(38);paint.setColor(fg[theme]);
        StaticLayout layout=StaticLayout.Builder.obtain(body,0,body.length(),paint,920).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL).setLineSpacing(18,1).setIncludePad(true).build();
        ArrayList<int[]> pages=new ArrayList<>();int start=0;
        while(start<layout.getLineCount()){int end=start+1;while(end<layout.getLineCount()&&layout.getLineBottom(end)-layout.getLineTop(start)<=1120)end++;pages.add(new int[]{start,end});start=end;}
        File dir=new File(ctx.getCacheDir(),"cards");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create share directory");
        // Keep recently shared files valid for receivers; remove only old exports.
        File[] old=dir.listFiles();if(old!=null)for(File f:old)if(System.currentTimeMillis()-f.lastModified()>86400000L)f.delete();
        ArrayList<Uri> uris=new ArrayList<>();String batch=UUID.randomUUID().toString();
        for(int i=0;i<pages.size();i++){
            Bitmap bitmap=Bitmap.createBitmap(1080,1440,Bitmap.Config.ARGB_8888);
            try{Canvas canvas=new Canvas(bitmap);canvas.drawColor(bg[theme]);Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);border.setColor(fg[theme]);border.setAlpha(70);border.setStrokeWidth(2);border.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(36,36,1044,1404,22,22,border);
                int[] page=pages.get(i);canvas.save();canvas.clipRect(80,110,1000,1230);canvas.translate(80,110-layout.getLineTop(page[0]));layout.draw(canvas);canvas.restore();
                paint.setTextSize(26);drawFooter(canvas,paint,title,1260);drawFooter(canvas,paint,"پارسه · گنجور | "+(i+1)+" / "+pages.size(),1320);paint.setTextSize(38);
                File file=new File(dir,batch+"-"+(i+1)+".png");try(OutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Image export failed");}uris.add(Uri.parse("content://"+ctx.getPackageName()+".cards/"+file.getName()));
            }finally{bitmap.recycle();}
        }
        Intent intent=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE);intent.setType("image/png");
        if(uris.size()==1)intent.putExtra(Intent.EXTRA_STREAM,uris.get(0));else intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);
        intent.putExtra(Intent.EXTRA_TEXT,title+"\nپارسه | گنجور\n"+source);
        ClipData clip=ClipData.newRawUri("پارسه",uris.get(0));for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));intent.setClipData(clip);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);return intent;
    }
    static void drawFooter(Canvas canvas,TextPaint paint,String text,int y){String fitted=TextUtils.ellipsize(text,paint,920,TextUtils.TruncateAt.END).toString();StaticLayout l=StaticLayout.Builder.obtain(fitted,0,fitted.length(),paint,920).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL).build();canvas.save();canvas.translate(80,y);l.draw(canvas);canvas.restore();}
}

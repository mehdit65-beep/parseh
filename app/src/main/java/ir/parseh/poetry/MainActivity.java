package ir.parseh.poetry;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final Handler main = new Handler(Looper.getMainLooper());
    Corpus corpus; LinearLayout root, content, bar; Typeface font;
    android.content.SharedPreferences prefs;
    int bg, ink, accent = Color.rgb(35,93,80), serial=0;
    String[] current; boolean dark; float size=21;
    int dp(int n){ return (int)(getResources().getDisplayMetrics().density*n); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=getSharedPreferences("personal",0);
        dark=prefs.getBoolean("dark",false); size=prefs.getFloat("size",21);
        try {font=Typeface.createFromAsset(getAssets(),"Vazirmatn.ttf");} catch(Exception e){font=Typeface.create("sans-serif",0);}
        screen("پارسه"); label("کتابخانه در حال آماده‌شدن است…",18);
        worker.execute(()->{try {corpus=new Corpus(this);main.post(()->{if(!isDestroyed())home();});}
            catch(Exception e){main.post(()->{screen("پارسه");label("بانک اشعار در این فایل نصب موجود یا قابل خواندن نیست.",18);});}});
    }
    void screen(String title) {
        serial++; bg=Color.parseColor(dark?"#172522":"#F7F4ED");ink=Color.parseColor(dark?"#F7F4ED":"#243B35");
        root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(bg);
        root.setPadding(dp(20),dp(12),dp(20),dp(10));
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(20),dp(12)+i.top,dp(20),dp(10)+i.bottom);return insets;});
        setContentView(root);
        TextView heading=text(title,28);heading.setPadding(0,dp(12),0,dp(12));root.addView(heading);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);scroll.setFillViewport(true);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        bar=new LinearLayout(this); root.addView(bar);
        nav("خانه",()->home());nav("جست‌وجو",()->search());nav("نشان‌ها",()->bookmarks());nav("تنظیمات",()->settings());
    }
    TextView text(String s,float sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(ink);t.setTypeface(font);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);t.setLineSpacing(dp(5),1.15f);return t;}
    TextView label(String s,float sp){TextView t=text(s,sp);t.setPadding(0,dp(10),0,dp(10));content.addView(t);return t;}
    void nav(String s,Runnable r){TextView t=text(s,12);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(16),0,dp(10));bar.addView(t,new LinearLayout.LayoutParams(0,-2,1));t.setOnClickListener(v->r.run());}
    void button(String s,Runnable r){TextView t=label(s,17);t.setPadding(dp(16),dp(15),dp(16),dp(15));GradientDrawable shape=new GradientDrawable();shape.setColor(dark?0xff223b34:0xffe9eade);shape.setCornerRadius(dp(14));t.setBackground(shape);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)t.getLayoutParams();lp.bottomMargin=dp(9);t.setLayoutParams(lp);t.setOnClickListener(v->r.run());}
    interface Rows {void accept(List<String[]> rows);}
    void load(String sql,Rows done,String... args){int token=serial;worker.execute(()->{try{List<String[]> rows=corpus.query(sql,args);main.post(()->{if(token==serial&&!isDestroyed())done.accept(rows);});}catch(Exception e){main.post(()->{if(token==serial)label("خواندن اطلاعات انجام نشد؛ دوباره تلاش کنید.",16);});}});}
    void home(){screen("پارسه");label("خانه‌ای برای شعر فارسی",18);
        int last=prefs.getInt("last",-1);if(last>0)button("ادامهٔ مطالعه",()->poem(last));
        label("شاعران",22);load("SELECT id,name FROM poets ORDER BY name",rows->{for(String[] r:rows)button(r[1],()->categories(Integer.parseInt(r[0]),null,r[1]));});}
    void categories(int poet,Integer parent,String title){screen(title);
        String sql=parent==null?"SELECT id,title FROM categories WHERE poet=? AND (parent IS NULL OR parent=0 OR parent NOT IN (SELECT id FROM categories)) ORDER BY id":"SELECT id,title FROM categories WHERE poet=? AND parent=? ORDER BY id";
        load(sql,rows->{for(String[] r:rows)button(r[1]+"  ‹",()->categories(poet,Integer.parseInt(r[0]),r[1]));if(parent!=null)poemList("SELECT id,title FROM poems WHERE category=? ORDER BY id",String.valueOf(parent));},parent==null?new String[]{""+poet}:new String[]{""+poet,""+parent});
    }
    void poemList(String sql,String... args){load(sql,rows->{if(rows.isEmpty())label("شعری یافت نشد.",16);else appendPage(rows,0);},args);}
    void appendPage(List<String[]> rows,int start){int end=Math.min(start+60,rows.size());for(int i=start;i<end;i++){String[] r=rows.get(i);button(r[1],()->poem(Integer.parseInt(r[0])));}if(end<rows.size()){button("نمایش بیشتر",()->{content.removeViewAt(content.getChildCount()-1);appendPage(rows,end);});}}
    void poem(int id){screen("در حال خواندن…");load("SELECT p.id,p.title,p.body,p.url,a.name,p.source FROM poems p JOIN poets a ON a.id=p.poet WHERE p.id=?",rows->{if(rows.isEmpty())return;current=rows.get(0);prefs.edit().putInt("last",id).apply();reader();},""+id);}
    void reader(){String[] p=current;screen(p[4]);label(p[1],22);if(p[2].trim().isEmpty()){label("متن این مدخل در نسخهٔ منبع گنجور موجود نیست.",18);return;}TextView body=label(p[2],size);body.setGravity(Gravity.CENTER);body.setTextIsSelectable(true);
        button("اشتراک‌گذاری کل شعر — متن",()->shareText(p[2],p));
        button("اشتراک‌گذاری کل شعر — تصویر",()->export(p[2],p,0));
        button("ساخت کارت‌پستال از شعر",()->postcard(p));
        boolean saved=prefs.getBoolean("bookmark_"+p[0],false);button(saved?"حذف نشان":"نشان‌کردن شعر",()->{prefs.edit().putBoolean("bookmark_"+p[0],!saved).apply();reader();});
        label("منبع: گنجور · "+p[5],12);
    }
    void postcard(String[] p){screen("کارت‌پستال");label("مصراع‌های دلخواه را انتخاب کنید",17);String[] lines=p[2].split("\n");ArrayList<CheckBox> checks=new ArrayList<>();
        for(String line:lines){CheckBox c=new CheckBox(this);c.setText(line);c.setTextColor(ink);c.setTypeface(font);c.setTextSize(17);content.addView(c);checks.add(c);}
        Spinner theme=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کاغذ و جوهر","سبز ایرانی","شب آرام"});theme.setAdapter(adapter);content.addView(theme);
        button("پیش‌نمایش و اشتراک‌گذاری",()->{StringBuilder s=new StringBuilder();for(CheckBox c:checks)if(c.isChecked()){if(s.length()>0)s.append('\n');s.append(c.getText());}if(s.length()==0){toast("حداقل یک مصراع انتخاب کنید");return;}previewCard(s.toString(),p,theme.getSelectedItemPosition());});
    }
    void previewCard(String body,String[] poem,int theme){screen("پیش‌نمایش کارت");int[] backgrounds={0xfff7f4ed,0xff235d50,0xff172522};int[] foregrounds={0xff243b35,0xfff7f4ed,0xffeee6d3};
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(24),dp(32),dp(24),dp(32));card.setBackgroundColor(backgrounds[theme]);content.addView(card);
        for(String value:new String[]{body,poem[4]+" · "+poem[1],"پارسه | گنجور"}){TextView t=text(value,value.equals(body)?21:13);t.setGravity(Gravity.CENTER);t.setTextColor(foregrounds[theme]);card.addView(t);}
        label("متن بلند در چند تصویر شماره‌دار خروجی گرفته می‌شود.",13);button("اشتراک‌گذاری تصویر",()->export(body,poem,theme));}
    void shareText(String body,String[] p){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,p[4]+" — "+p[1]+"\n\n"+body+"\n\nپارسه | گنجور\n"+p[3]);startActivity(Intent.createChooser(i,"اشتراک‌گذاری شعر"));}
    void export(String body,String[] p,int theme){toast("در حال آماده‌سازی تصویر…");worker.execute(()->{try{Intent i=Cards.create(this,font,body,p[4]+" · "+p[1],p[3],theme);main.post(()->{if(!isDestroyed())startActivity(Intent.createChooser(i,"اشتراک‌گذاری تصویر"));});}catch(Exception|OutOfMemoryError e){main.post(()->toast("ساخت تصویر انجام نشد؛ فضای دستگاه را بررسی کنید."));}});}
    void bookmarks(){screen("نشان‌شده‌ها");ArrayList<String> ids=new ArrayList<>();for(String key:prefs.getAll().keySet())if(key.startsWith("bookmark_")&&prefs.getBoolean(key,false))ids.add(key.substring(9));if(ids.isEmpty()){label("هنوز شعری نشان نکرده‌اید.",17);return;}String marks=String.join(",",Collections.nCopies(ids.size(),"?"));poemList("SELECT id,title FROM poems WHERE id IN ("+marks+") ORDER BY id",ids.toArray(new String[0]));}
    void search(){screen("جست‌وجوی آفلاین");EditText input=new EditText(this);input.setHint("واژه یا بخشی از شعر");input.setTypeface(font);input.setTextColor(ink);input.setHintTextColor(0xff888888);content.addView(input);
        button("جست‌وجو در اشعار",()->{String q=Corpus.match(input.getText().toString());if(q.isEmpty())return;screen("نتیجهٔ جست‌وجو");label("تا ۲۰۰ نتیجه؛ برای نتیجهٔ دقیق‌تر عبارت را کامل کنید.",13);poemList("SELECT p.id,a.name || ' · ' || p.title FROM search JOIN poems p ON p.id=search.docid JOIN poets a ON a.id=p.poet WHERE search MATCH ? LIMIT 200",q);});}
    void settings(){screen("تنظیمات");button(dark?"ظاهر روشن":"ظاهر تاریک",()->{dark=!dark;prefs.edit().putBoolean("dark",dark).apply();settings();});
        label("اندازهٔ متن شعر",17);SeekBar seek=new SeekBar(this);seek.setMax(16);seek.setProgress((int)size-16);content.addView(seek);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){size=16+v;prefs.edit().putFloat("size",size).apply();}public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});
        button(prefs.getBoolean("reminder",false)?"خاموش‌کردن یادآوری روزانه":"فعال‌کردن یادآوری روزانه",()->{if(prefs.getBoolean("reminder",false)){prefs.edit().putBoolean("reminder",false).apply();ReminderReceiver.schedule(this);settings();return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);return;}pickTime();});
        label("یادآوری روزانه روی همین دستگاه اجرا می‌شود. زمان نمایش ممکن است با مدیریت باتری تغییر کند.",14);
        if(BuildConfig.PUSH_ENABLED)button(prefs.getBoolean("push",false)?"خاموش‌کردن پیام‌های پارسه":"دریافت پیام‌های پارسه",()->{
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8);return;}
            togglePush();
        });else label("پیام‌های ارسالی پارسه در این نسخه فعال نیست.",14);
        label("پارسه · نسخهٔ آزمایشی ۰٫۱\nاشعار: ganjoor.net\nقلم: وزیرمتن — مجوز OFL\nبدون حساب کاربری و بدون ارسال اطلاعات شخصی",14);
    }
    void pickTime(){new TimePickerDialog(this,(v,h,m)->{prefs.edit().putInt("hour",h).putInt("minute",m).putBoolean("reminder",true).apply();ReminderReceiver.schedule(this);settings();},prefs.getInt("hour",20),prefs.getInt("minute",0),true).show();}
    void togglePush(){try{Class.forName("ir.parseh.poetry.PushBridge").getMethod("toggle",MainActivity.class).invoke(null,this);}catch(Exception e){toast("سرویس پیام در دسترس نیست.");}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED){if(request==7)pickTime();else if(request==8)togglePush();}else toast("مجوز اعلان داده نشد؛ می‌توانید بعداً فعال کنید.");}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(corpus!=null)home();else super.onBackPressed();}
    @Override public void onDestroy(){serial++;worker.shutdown();super.onDestroy();}
}

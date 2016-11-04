package com.appvisor_event.master;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by BraveSoft on 16/10/20.
 */
public class CustomImageView extends FrameLayout {

    private View view;
    private ImageView image_root,image_up;
    private Context mContext;
    private int width,heigth, img_width;

    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        view= LayoutInflater.from(context).inflate(R.layout.custom_imageview,this);
        this.mContext=context;
        initView(view);
    }

    private void initView(View view) {
        image_root= (ImageView) view.findViewById(R.id.img_root);
        image_up= (ImageView) view.findViewById(R.id.img_up);
        ViewTreeObserver viewTreeObserver=image_root.getViewTreeObserver();

            WindowManager wm = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);

             width = wm.getDefaultDisplay().getWidth();
            heigth = wm.getDefaultDisplay().getHeight();

    }

    public void addImage(ArrayList<ImageItem> items){

        Bitmap bitmap1=null;
        Bitmap firstBitmap=null;

        if (items!=null||items.size()>0){
            String path= items.get(0).getName();
            File mFile=new File(path);
            //若该文件存在
            if (mFile.exists()) {
                firstBitmap=BitmapFactory.decodeFile(path);
            }
            bitmap1 = Bitmap.createBitmap(width, width,
                    Bitmap.Config.RGB_565);
            Bitmap bitmap_icon=null;
            Bitmap bitmap = null;
            Log.d("frame",items.size()+"img_size");
            Canvas canvas = new Canvas(bitmap1);
            canvas.drawBitmap(firstBitmap, new Matrix(), null);
            for (int i = 1; i <items.size() ; i++) {
                String p = getContext().getFilesDir().toString() + "/images/" + items.get(i).getName();//图片路径
                File file = new File(p);
                //若该文件存在

                if (file.exists()) {

                    bitmap_icon = BitmapFactory.decodeFile(p);
                }
                if (bitmap_icon != null) {
                    img_width = bitmap_icon.getWidth();
                }else {

                }

                float scale = 0.0f;
                scale = (float) ((width * items.get(i).getScale()) / img_width);
                bitmap = small(bitmap_icon, scale);
                Log.d("PATH", bitmap.getWidth()+"---"+bitmap.getHeight());
                canvas.drawBitmap(bitmap, (int) ((width * items.get(i).getWidth_position())), (int) ((width * items.get(i).getHeight_position())), null);
                firstBitmap = bitmap1;
                bitmap_icon = null;
                bitmap = null;
            }
            image_root.setImageBitmap(bitmap1);


        }
    }

    private  Bitmap small(Bitmap bitmap,float size) {
        Matrix matrix = new Matrix();
        matrix.postScale(size,size);
        int bm_width=bitmap.getWidth();
        int bm_height=bitmap.getHeight();
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0,bm_width,
                bm_height, matrix, true);
        return resizeBmp;
    }

}

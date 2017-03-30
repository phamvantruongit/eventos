package com.appvisor_event.master.modules.Document;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.appvisor_event.master.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by bsfuji on 2017/03/29.
 */

public class Document
{
    private Context    context   = null;
    private List<Item> documents = new ArrayList<>();

    public Document(Context context)
    {
        this.context = context;

        loadDocuments();
    }

    public static class Item
    {
        public class Category
        {
            private String id   = null;
            private String name = null;

            public Category(String id, String name)
            {
                this.id   = id;
                this.name = name;
            }

            public String getId()
            {
                return this.id;
            }

            public String getName()
            {
                return this.name;
            }
        }

        public class Period
        {
            private String startDate = null;
            private String endDate = null;

            public Period(String startDate, String endDate)
            {
                this.startDate = startDate;
                this.endDate   = endDate;
            }

            public String getStartDate()
            {
                return this.startDate;
            }

            public String getEndDate()
            {
                return this.endDate;
            }

            public boolean isWithin()
            {
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

                    Date startDate = simpleDateFormat.parse(this.startDate);
                    Date endDate   = simpleDateFormat.parse(this.endDate);
                    Date nowDate   = new Date();

                    return (nowDate.equals(startDate) || nowDate.equals(endDate) || (startDate.after(nowDate) && endDate.before(nowDate)));
                }
                catch (ParseException exception) {}

                return false;
            }
        }

        private String   id                 = null;
        private Category category           = null;
        private String   name               = null;
        private Period   period             = null;
        private String   sequence           = null;
        private String   thumbnailImagePath = null;
        private String   dataPath           = null;

        public Item(String jsonString) throws JSONException
        {
            init(new JSONObject(jsonString));
        }

        public Item(JSONObject json) throws JSONException
        {
            init(json);
        }

        public String getId()
        {
            return this.id;
        }

        public Category getCategory()
        {
            return this.category;
        }

        public String getName()
        {
            return this.name;
        }

        public Period getPeriod()
        {
            return this.period;
        }

        public String getSequence()
        {
            return this.sequence;
        }

        public String getThumbnailImagePath()
        {
            return this.thumbnailImagePath;
        }

        public String getDataPath()
        {
            return this.dataPath;
        }

        public boolean isPublic()
        {
            return this.period.isWithin();
        }

        public boolean equals(Object object)
        {
            if (null == object)
            {
                return false;
            }

            if (!(object instanceof Item))
            {
                return false;
            }

            return ((Item)object).getId() == this.id;
        }

        public String toString()
        {
            try {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("id",                            this.id);
                jsonObject.put("event_document_category_id",    this.category.getId());
                jsonObject.put("event_document_category_name",  this.category.getName());
                jsonObject.put("name",                          this.name);
                jsonObject.put("period_start_date",             this.period.getStartDate());
                jsonObject.put("period_end_date",               this.period.getEndDate());
                jsonObject.put("sequence",                      this.sequence);
                jsonObject.put("thumbnail_image_path",          this.thumbnailImagePath);
                jsonObject.put("path",                          this.dataPath);

                return jsonObject.toString();
            } catch (JSONException exception) {
                Log.e("tto", "exception: " + exception.getMessage());
            }

            return null;
        }

        private void init(JSONObject json) throws JSONException
        {
            this.id                 = json.getString("id");
            this.category           = new Category(json.getString("event_document_category_id"), json.getString("event_document_category_name"));
            this.name               = json.getString("name");
            this.period             = new Period(json.getString("period_start_date"), json.getString("period_end_date"));
            this.sequence           = json.getString("sequence");
            this.thumbnailImagePath = json.getString("thumbnail_image_path");
            this.dataPath           = json.getString("path");
        }
    }

    public static Item newItem(JSONObject jsonObject) throws JSONException
    {
        return new Item(jsonObject);
    }

    public static Boolean isDocumentUrl(URL url)
    {
        return url.getPath().startsWith(String.format("/%s/documents", Constants.Event));
    }

    public boolean isSavedItem(Item item)
    {
        return documents.contains(item);
    }

    private void loadDocuments()
    {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("Documents", Context.MODE_PRIVATE);
            String items = sharedPreferences.getString("items", "[]");

            JSONArray jsonDocuments = new JSONArray(items);
            for (int i = 0; i < jsonDocuments.length(); i++)
            {
                JSONObject jsonDocument = jsonDocuments.getJSONObject(i);
                documents.add(new Item(jsonDocument));
            }
        } catch (JSONException exception) {}
    }

    public List<Item> getDocuments()
    {
        return documents;
    }

    public void saveDocument(Item item)
    {
        if (isSavedItem(item))
        {
            documents.remove(item);
        }
        documents.add(item);

        saveDocuments();
    }

    public void saveDocuments()
    {
        try {
            JSONArray jsonDocuments = new JSONArray();
            for (Item item : documents)
            {
                JSONObject jsonDocument = new JSONObject(item.toString());
                jsonDocuments.put(jsonDocument);
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences("Documents", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("items", jsonDocuments.toString());
            editor.commit();
        } catch (JSONException exception) {}
    }
}

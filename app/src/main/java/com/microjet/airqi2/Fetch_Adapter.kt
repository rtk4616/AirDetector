package com.microjet.airqi2import android.view.Viewimport android.view.ViewGroupimport android.widget.BaseAdapterimport android.widget.TextViewimport java.util.*class Fetch_Adapter (val time_list: ArrayList<String>,val list: ArrayList<String>, val context: FetchDataMain) : BaseAdapter() {    override fun getCount(): Int {        return list.size    }    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {        var holder: DataViewHolder        var v: View        if (convertView == null) {            v = View.inflate(context, R.layout.fetch_adapter, null)            holder = DataViewHolder(v)            v.tag = holder        } else {            v = convertView            holder = v.tag as DataViewHolder        }        holder.str.text = list[position]        holder.str_time.text = time_list[position]        return v    }    override fun getItem(position: Int): Any? {        return time_list.get(position)    }    //    override fun getItemInfo(position: Int): Any? {//        return list.get(position)//    }    override fun getItemId(position: Int): Long {        return position.toLong()    }}class DataViewHolder(var viewItem: View) {    var str: TextView = viewItem.findViewById(R.id.facth_info_Date) as TextView    var str_time: TextView = viewItem.findViewById(R.id.facth_time_Date) as TextView}
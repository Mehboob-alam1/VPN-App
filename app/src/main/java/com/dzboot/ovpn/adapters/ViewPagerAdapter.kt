package com.dzboot.ovpn.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dzboot.ovpn.R

class ViewPagerAdapter(private var title:List<String>,private var details:List<String>,private var Images:List<Int>) :RecyclerView.Adapter<ViewPagerAdapter.Pager2ViewHolder>() {

  inner class Pager2ViewHolder(itemView :View) :RecyclerView.ViewHolder(itemView){
      val itemTitle: TextView =itemView.findViewById(R.id.viewPagerTitle)
      val itemDesc: TextView =itemView.findViewById(R.id.viewPagerDetails)
      val itemimages: ImageView =itemView.findViewById(R.id.viewPagerImage)
      init {

      }
  }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerAdapter.Pager2ViewHolder {
       return Pager2ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_view_pager,parent,false))
    }

    override fun onBindViewHolder(holder: ViewPagerAdapter.Pager2ViewHolder, position: Int) {
   holder.itemTitle.text=title[position]
   holder.itemDesc.text=details[position]

        holder.itemimages.setImageResource(Images[position])
    }

    override fun getItemCount(): Int {
        return title.size
    }
}
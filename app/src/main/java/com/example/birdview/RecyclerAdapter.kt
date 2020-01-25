package com.example.birdview

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.product_item.view.*
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

class RecyclerAdapter(var mList:ArrayList<RecyclerItem>) :RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>(){

    private lateinit var clickListener: ItemClickListener
    private var myList:ArrayList<RecyclerItem>
    init {
        myList=mList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)= MyViewHolder(parent)

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        mList[position].let { item -> with(holder){

            holder.itemView.setOnClickListener{
                clickListener.onItemClick(item)
        }
            Glide.with(image).load(item.image).into(image)
            title.text=item.title
            val numberFormat="${NumberFormat.getNumberInstance(Locale.KOREA).format(item.price.toInt())}원"
            price.text=numberFormat
        } }
    }

    interface ItemClickListener{
        fun onItemClick(item: RecyclerItem)
    }

    fun setItemClickListener(itemClickListener:ItemClickListener){
        this.clickListener=itemClickListener
    }

    inner class MyViewHolder(parent: ViewGroup):RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)){
        val image:ImageView=itemView.productimageView
        val title:TextView=itemView.title
        val price:TextView=itemView.price
    }
}
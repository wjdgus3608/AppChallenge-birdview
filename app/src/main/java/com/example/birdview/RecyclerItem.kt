package com.example.birdview

data class RecyclerItem(val id:Int, val price:String,val image:String, val title:String,val oily_score:Int,val dry_score:Int, val sensitive_score:Int, val description:String){
    constructor(id:Int, price:String, image:String, title:String, oily_score: Int):this(id,price,image,title,oily_score,-1,-1,"")
}
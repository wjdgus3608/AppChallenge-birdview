package com.example.birdview

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.detail.view.*
import kotlinx.android.synthetic.main.progress_bar.*
import androidx.test.espresso.idling.CountingIdlingResource
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity() {
    private lateinit var popupWindow:PopupWindow
    companion object{
        val idlingResource=CountingIdlingResource("async")
        val searchType= arrayOf("oily","oily","dry","sensitive")
        val dataList= ArrayList<RecyclerItem>()
        val cache= ArrayList<RecyclerItem>()
        val detailCache= HashMap<Int,RecyclerItem>()
        val requestCache= HashMap<String,ArrayList<RecyclerItem>>()
        val adapter=RecyclerAdapter(dataList)
        lateinit var taskQueue:Queue<AsyncTask<String,Void,Int>>
        lateinit var popupView:View
        lateinit var layoutManager:GridLayoutManager
        lateinit var listener:CustomEventListener
        var page_numbers= arrayOf(1,1,1,1)
        var searchMode:Int=0
        var searchKeyword:String?=null
        var cacheIdx:Int=0
        var loading:Boolean=false
        var cacheEnd:Boolean=false
        fun pullDataFromCache(){
            for (i in 0..19){
                if(i+ cacheIdx>=cache.size){
                    listener.loadingEnd()
                    cacheEnd=true
                    return
                }
                dataList.add(cache[i+cacheIdx])
            }
            Log.e("log","load item!!")
            Log.e("log","cash size : ${cache.size}" )
            Log.e("log","cash idx : $cacheIdx")
            cacheIdx+=20
            adapter.notifyDataSetChanged()
            listener.loadingEnd()

        }
        fun pullDetailFromCache(id:Int){
            val item:RecyclerItem= detailCache[id]!!
            Glide.with(popupView.detail_img).load(item.image).into(popupView.detail_img)
            popupView.detail_title.text=item.title
            val priceNumberFormat="${NumberFormat.getNumberInstance(Locale.KOREA).format(item.price.toInt())}원"
            popupView.detail_price.text=priceNumberFormat
            popupView.detail_des.text=item.description.replace("\\n","\n")
            listener.loadingEnd()
        }
    }

    interface CustomEventListener{
        fun doLoading()
        fun loadingEnd()
        fun networkError(msg:String, mode:Int)
        fun dataExist()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        taskQueue=LinkedList()
        val im:InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        recyclerView.adapter= adapter
        layoutManager=GridLayoutManager(this,2)
        recyclerView.layoutManager=layoutManager

        listener=object :CustomEventListener{
            override fun doLoading() {
                loading=true
                Log.e("log", "loading : $loading")
                loading_view.visibility=View.VISIBLE
            }

            override fun loadingEnd() {
                loading=false
                Log.e("log", "loading : $loading")
                loading_view.visibility=View.GONE
            }

            override fun networkError(msg:String, mode:Int) {
                if(mode==0) {
                    errormsg.visibility = View.VISIBLE
                }
                Toast.makeText(application, msg, Toast.LENGTH_LONG).show()
            }

            override fun dataExist() {
                errormsg.visibility=View.INVISIBLE
            }

        }

        searchBtn.setOnClickListener{
                Log.e("log","search btn click")
                searchKeyword=search_text_view.text.toString()
                clearDataList()
                initPageNumber()
                clearCache()
                initScrollPosition()
                requestData(searchMode)
                search_text_view.clearFocus()
                im.hideSoftInputFromWindow(main_container.windowToken,0)
                bottom.visibility=View.VISIBLE
        }

        search_text_view.setOnTouchListener{_,_->
                bottom.visibility=View.INVISIBLE
                clearTask()
                search_text_view.requestFocus()
                im.showSoftInput(search_text_view,0)
                true
        }

        main_container.setOnClickListener{
                Log.e("log","bottom click!")
                search_text_view.clearFocus()
                im.hideSoftInputFromWindow(main_container.windowToken,0)
                bottom.visibility=View.VISIBLE
        }

        val options=resources.getStringArray(R.array.options)
        val spinnerAdapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options)
        spinner.adapter=spinnerAdapter
        spinner.setSelection(0,false)
        spinner.onItemSelectedListener= object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                clearTask()
                clearDataList()
                clearCache()
                initPageNumber()
                initScrollPosition()
                searchMode=position
                requestData(searchMode)
            }
        }

        adapter.setItemClickListener(object:RecyclerAdapter.ItemClickListener{
            override fun onItemClick(item: RecyclerItem) {
                Log.e("log",item.title)
                Log.e("log",item.id.toString())
                openDetailWindow()
                if(!detailCache.containsKey(item.id)) requestDetail(item.id)
                pullDetailFromCache(item.id)
            }})

        recyclerView.addOnScrollListener(object :RecyclerView.OnScrollListener(){
            var locate:Int=0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if(dy!=0){
                    locate+=dy
                }
                if (300<locate && locate<=spinner_container.height+300){
                    Log.e("log","over")
                    spinner_container.animate().translationY(-(locate-300).toFloat()).setInterpolator(AccelerateInterpolator(0f)).withLayer()
                }
                else if(spinner_container.translationY.absoluteValue<spinner_container.height+300){
                    val value=if(locate<=300) { 0.0f} else -spinner_container.height.toFloat()
                    spinner_container.animate().translationY(value).interpolator=AccelerateInterpolator(0f)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val lastItemPosition:Int=layoutManager.findLastVisibleItemPosition()
                val totalItemCount:Int=layoutManager.itemCount
                if(!loading && cacheEnd) requestData(searchMode)
                if(!loading && lastItemPosition==totalItemCount-1){
                    listener.doLoading()
                    loadMoreData()
                }
            }
        })

        requestData(0)
    }

    override fun onBackPressed() {
        Log.e("log","back pressed!")
        if(popupWindow.isShowing) {
            wrapper.visibility=View.INVISIBLE
            popupWindow.dismiss()
        }
    }

    fun requestData(mode:Int){
        val currentType=searchType[mode]
        val pageNum= page_numbers[mode]++
        listener.doLoading()
        if(searchKeyword.isNullOrBlank()){
            taskQueue.add(DownloadItems().execute("/products?skin_type=$currentType&page=$pageNum",currentType,"0"))
        }
        else{
            taskQueue.add(DownloadItems().execute("/products?skin_type=$currentType&page=$pageNum&search=$searchKeyword",currentType,"0"))
        }
    }

    fun requestDetail(id:Int){
        listener.doLoading()
        DownloadItems().execute("/products/$id","","1").get()
    }

    fun loadMoreData()= pullDataFromCache()

    fun openDetailWindow(){

        wrapper.visibility=View.VISIBLE
        val inflater = LayoutInflater.from(this)
        popupView=inflater.inflate(R.layout.detail,null)
        val exitBtn=popupView.findViewById<View>(R.id.exitBtn)
        popupWindow=PopupWindow(popupView,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        exitBtn.setOnClickListener{
                wrapper.visibility=View.INVISIBLE
                popupWindow.dismiss()
        }
        popupWindow.animationStyle=R.style.Animation
        popupWindow.showAtLocation(main_container,Gravity.CENTER,0,0)
    }

    fun clearDataList() = dataList.clear()
    fun clearCache(){
        cache.clear()
        cacheIdx=0
    }
    fun clearTask() {
        if(taskQueue.size>0){
            for (task in taskQueue){
                task.cancel(true)
            }
        }
    }
    fun initPageNumber(){ page_numbers= arrayOf(1,1,1,1) }
    fun initScrollPosition() = layoutManager.scrollToPosition(0)
    fun getCache() = cache


    class DownloadItems : AsyncTask<String,Void,Int>(){

        private val baseURL="https://6uqljnm1pb.execute-api.ap-northeast-2.amazonaws.com/prod"

        override fun onPreExecute() {
            super.onPreExecute()
            idlingResource.increment()
        }

        override fun doInBackground(vararg params: String?): Int {
            val url=baseURL+params[0]
            if(requestCache.containsKey(url) && params[2].equals("0")){//이미 요청했던 url일 경우 캐시에서 넘겨줌

                listener.dataExist()
                val tmpList=requestCache[url]
                for(idx in 0 until tmpList!!.size){
                    cache.add(tmpList[idx])
                }
                cacheEnd=false
                Log.e("log","tmpList size : "+tmpList.size)
                Log.e("log","Cache size : "+cache.size)
                Log.e("log","RequestCache size : "+ requestCache.size)
                return 0
            }
            val con= URL(url).openConnection() as HttpURLConnection
            Log.e("log","request with -> $url")
            con.requestMethod="GET"
            con.setRequestProperty("Content-Type", "application/json")
            try {
                Log.e("log","Background Task start")
                val dataText=con.inputStream.bufferedReader().readText()
                val json=JSONObject(dataText)
                val statusCode=json.get("statusCode")
                Log.e("log","statusCode : $statusCode")
                when(statusCode){
                    200->{
                        listener.dataExist()
                        parseJSONbyMode(params,json)
                        cacheEnd=false
                        return params[2]!!.toInt()
                    }
                    400->{
                        Handler(Looper.getMainLooper()).post{
                            listener.networkError("클라이언트 요청 오류입니다.",params[2]!!.toInt())
                        }
                        listener.loadingEnd()
                        return -1
                    }
                    404->{
                        Handler(Looper.getMainLooper()).post{
                            listener.networkError("조회된 데이터가 없습니다.",params[2]!!.toInt())
                        }
                        listener.loadingEnd()
                        return -1
                    }
                    500->{
                        Handler(Looper.getMainLooper()).post{
                            listener.networkError("서버 오류가 발생했습니다.",params[2]!!.toInt())
                        }
                        listener.loadingEnd()
                        return -1
                    }
                }
            }
            catch (ex:Exception){
                println(ex)
                return -1
            }
            con.disconnect()
            return -1
        }

        override fun onPostExecute(result: Int) {
            Log.e("log","Background Task end")
            super.onPostExecute(result)
            if(result==0)
                pullDataFromCache()
            taskQueue.poll()
            idlingResource.decrement()
        }

        override fun onCancelled() {
            super.onCancelled()
            Log.e("log","task cancelled!")
        }

        private fun parseJSONbyMode(params: Array<out String?>, json:JSONObject){
            if (!isCancelled) {
                when (params[2]!!.toInt()) {
                    0 -> {
                        val datas = json.getJSONArray("body") as JSONArray
                        val tmpList = ArrayList<RecyclerItem>()
                        for (idx in 0 until datas.length()) {
                            val item = datas.getJSONObject(idx)
                            val id = item.get("id") as Int
                            val price = item.get("price").toString()
                            val imageUrl = item.get("thumbnail_image").toString()
                            val oilyScore = item.get(params[1] + "_score") as Int
                            val title = item.get("title").toString()
                            cache.add(RecyclerItem(id, price, imageUrl, title, oilyScore))
                            tmpList.add(RecyclerItem(id, price, imageUrl, title, oilyScore))
                            println(item)
                        }
                        Log.e("log", "added at : " + baseURL + params[0])
                        requestCache[baseURL + params[0]] = tmpList

                    }
                    1 -> {
                        val item = json.getJSONObject("body")
                        val id = item.get("id") as Int
                        val price = item.get("price").toString()
                        val imageUrl = item.get("full_size_image").toString()
                        val oilyScore = item.get("oily_score") as Int
                        val dryScore = item.get("dry_score") as Int
                        val sensitiveScore = item.get("sensitive_score") as Int
                        val title = item.get("title").toString()
                        val description = item.get("description").toString()
                        detailCache[id] = RecyclerItem(
                            id,
                            price,
                            imageUrl,
                            title,
                            oilyScore,
                            dryScore,
                            sensitiveScore,
                            description
                        )
                        println(item)
                    }
                }
            }
        }
    }
}

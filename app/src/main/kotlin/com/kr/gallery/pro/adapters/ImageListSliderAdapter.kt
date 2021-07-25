package com.kr.gallery.pro.adapters
    //Expression string of matching media list which has modified date same as date

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.kr.gallery.pro.R
import com.kr.gallery.pro.extensions.getImageType
import com.kr.gallery.pro.extensions.loadImage
import com.kr.gallery.pro.models.SliderItem
import com.smarteist.autoimageslider.SliderViewAdapter
import kotlinx.android.synthetic.main.image_item.view.*

/**
 * 사진첩보기화면에서 날자화상 sliderview에 리용되는 adapter
 * @param context
 * @param mSlideItems: adapter 자료
 * @param mItemClickCallback: click했을때 호출되는 callbacks
 */
class ImageListSliderAdapter(private val context: Context, private val mSlideItems: ArrayList<SliderItem>,
                             private val mItemClickCallback: (() -> Unit)?)
    : SliderViewAdapter<ImageListSliderAdapter.ViewHolder>(){

    //Layout inflater
    private val mLayoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val itemView = mLayoutInflater.inflate(R.layout.image_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, position: Int) {
        val sliderItem = mSlideItems[position]
        viewHolder?.bindView(sliderItem)
    }

    override fun onDestroyViewHolder(viewHolder: ViewHolder?) {
        viewHolder?.destroyView()
    }

    override fun getCount(): Int {
        return mSlideItems.count()
    }

    open inner class ViewHolder(itemView: View): SliderViewAdapter.ViewHolder(itemView) {
        fun bindView(slideItem: SliderItem){
            //해당한 image를 설정한다.
            val slideImage:ImageView = itemView.findViewById(R.id.iv_auto_image_slider)
            context.loadImage(slideItem.imagePath.getImageType(), slideItem.imagePath, slideImage)

            //Click사건동작을 추가한다.
            itemView.setOnClickListener {
                mItemClickCallback?.invoke()
            }
        }

        //Glide기억공간을 해제한다.
        fun destroyView() {
            val imageView = itemView.iv_auto_image_slider
            Glide.with(itemView.context).clear(imageView)
        }
    }
}

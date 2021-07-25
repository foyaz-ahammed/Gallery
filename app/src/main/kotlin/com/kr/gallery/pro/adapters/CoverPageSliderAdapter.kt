package com.kr.gallery.pro.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.kr.gallery.pro.R
import com.kr.gallery.pro.models.SliderItem
import com.smarteist.autoimageslider.SliderViewAdapter
import kotlinx.android.synthetic.main.coverpage_image_item.view.*

/**
 * Cover page의 sliderview에서 리용하는 adapter믈라스
 */
class CoverPageSliderAdapter: SliderViewAdapter<CoverPageSliderAdapter.SliderAdapterVH>() {
    private var mSliderItems = ArrayList<SliderItem>()
    var mItemClickCallback: ((Any) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup): SliderAdapterVH {
        val inflate: View = LayoutInflater.from(parent.context).inflate(R.layout.coverpage_image_item, parent, false)
        return SliderAdapterVH(inflate)
    }

    override fun onBindViewHolder(viewHolder: SliderAdapterVH, position: Int) {
        val sliderItem: SliderItem = mSliderItems[position]

        viewHolder.bindView(sliderItem) { itemView, adapterPosition ->
            setupView(itemView, sliderItem)
        }
    }

    //Adapter자료 설정
    fun setupAdapterData(sliderItems: ArrayList<SliderItem>, callback: ((Any) -> Unit)?) {
        mSliderItems = sliderItems
        mItemClickCallback = callback
    }

    private fun setupView(view: View, sliderItem: SliderItem) {
        view.apply {
            tv_auto_image_slider.text = sliderItem.dateTimeStr
            tv_auto_image_slider.textSize = 16f
            tv_auto_image_slider.setTextColor(Color.WHITE)

            val options = RequestOptions().format(DecodeFormat.PREFER_RGB_565).fitCenter()
            Glide.with(view.context)
                    .load(sliderItem.imagePath)
                    .apply(options)
                    .into(iv_auto_image_slider)
        }
    }

    private fun destroyView(view: View) {
        val imageView = view.iv_auto_image_slider
        Glide.with(view.context).clear(imageView)
    }

    override fun onDestroyViewHolder(viewHolder: SliderAdapterVH?) {
        viewHolder?.itemView?.let { destroyView(it) }
    }

    override fun getCount(): Int {
        return mSliderItems.size
    }

    open inner class SliderAdapterVH(itemView: View) : ViewHolder(itemView) {
        fun bindView(any: Any, callback: (itemView: View, adapterPosition: Int) -> Unit): View{
            return itemView.apply {
                callback(this, getItemPosition(any))
                setOnClickListener { viewClicked(any) }
            }
        }

        private fun viewClicked(any: Any) {
            mItemClickCallback?.invoke(any)
        }
    }
}

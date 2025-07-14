package com.example.catapulta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter(private val items: List<App>, private val onItemClick: (App) -> Unit) :
    RecyclerView.Adapter<Adapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.item_icon)
        val textView: TextView = view.findViewById(R.id.item_text)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])  // <-- call the lambda on click
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val app = items[position]
        holder.textView.text = app.name
        if (app.icon != null) {
            holder.iconView.setImageDrawable(app.icon)
        } else {
            holder.iconView.setImageResource(R.drawable.ic_default_icon) // fallback icon
        }
    }

    override fun getItemCount() = items.size
}

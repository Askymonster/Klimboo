package com.example.klimboo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klimboo.data.FirebaseQueries.Locker
import com.example.klimboo.data.PhotoManager

// ── Gerencia buscadores e listas de estoque ──────────────────────────────────────────────────────────────

class LockerSearchAdapter(
    context: Context,
    lockers: List<Locker>
) : ArrayAdapter<Locker>(context, 0, lockers.toMutableList()) {

    private val allLockers = lockers.toList()
    private val filteredLockers = lockers.toMutableList()

    override fun getCount(): Int = filteredLockers.size

    override fun getItem(position: Int): Locker? = filteredLockers.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        createView(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        createView(position, convertView, parent)

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.trim()?.lowercase().orEmpty()
            val results = if (query.isEmpty()) {
                allLockers
            } else {
                allLockers.filter {
                    it.name.lowercase().contains(query) || it.local.lowercase().contains(query)
                }
            }
            return FilterResults().apply {
                values = results
                count = results.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredLockers.clear()
            filteredLockers.addAll(results?.values as? List<Locker> ?: emptyList())
            notifyDataSetChanged()
        }
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_locker, parent, false)
        val locker = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.txtNameLocker).text = locker.name
        view.findViewById<TextView>(R.id.txtLocalLocker).text = locker.local

        val img = view.findViewById<ImageView>(R.id.imgLocker)
        if (locker.photoUrl != null) {
            val bitmap = PhotoManager.base64ToBitmap(locker.photoUrl)
            if (bitmap != null) img.setImageBitmap(bitmap) else img.setImageDrawable(null)
        } else {
            img.setImageDrawable(null)
        }

        return view
    }
}

class StockAdapter(
    private val context: Context,
    private val items: List<StockItem>
) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

    data class StockItem(
        val id: String,
        val name: String,
        val lockerName: String,
        val lockerLocal: String,
        val photoUrl: String?
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPhoto: ImageView = view.findViewById(R.id.imgPhoto)
        val txtName: TextView = view.findViewById(R.id.txtName)
        val txtLocker: TextView = view.findViewById(R.id.txtLocker)
        val txtLocal: TextView = view.findViewById(R.id.txtLocal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_locker, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name

        holder.txtLocker.text = context.getString(R.string.locker_format, item.lockerName)
        holder.txtLocker.visibility = View.VISIBLE

        holder.txtLocal.text = context.getString(R.string.local_locker, item.lockerLocal)
        holder.txtLocal.visibility = View.VISIBLE

        if (item.photoUrl != null) {
            val bitmap = PhotoManager.base64ToBitmap(item.photoUrl)
            if (bitmap != null) holder.imgPhoto.setImageBitmap(bitmap)
            else holder.imgPhoto.setImageDrawable(null)
        } else {
            holder.imgPhoto.setImageDrawable(null)
        }
    }
}

package com.example.klimboo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klimboo.data.FirebaseQueries.Locker
import com.example.klimboo.data.PhotoManager

// ── Gerencia o dropdown em StockPage e os itens em MainActivity ──────────────────────────────────────────────────────────────

class LockerSpinnerAdapter(
    context: Context,
    private val lockers: List<Locker>
) : ArrayAdapter<Locker>(context, 0, lockers) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        createView(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        createView(position, convertView, parent)

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_locker, parent, false)
        val locker = lockers[position]
        view.findViewById<TextView>(R.id.txtNameLocker).text = locker.name
        view.findViewById<TextView>(R.id.txtLocalLocker).text = locker.local  // Adiciona aqui
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
    private val items: List<Locker>
) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

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
        val locker = items[position]
        holder.txtName.text = locker.name

        holder.txtLocker.text = context.getString(R.string.locker_format, locker.name)
        holder.txtLocker.visibility = View.VISIBLE

        holder.txtLocal.text = context.getString(R.string.local_locker, locker.local)
        holder.txtLocal.visibility = View.VISIBLE

        if (locker.photoUrl != null) {
            val bitmap = PhotoManager.base64ToBitmap(locker.photoUrl)
            if (bitmap != null) holder.imgPhoto.setImageBitmap(bitmap)
            else holder.imgPhoto.setImageDrawable(null)
        } else {
            holder.imgPhoto.setImageDrawable(null)
        }
    }
}
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
        view.findViewById<TextView>(R.id.txtNomeArmario).text = locker.name
        val img = view.findViewById<ImageView>(R.id.imgArmario)
        if (locker.photoUrl != null) {
            val bitmap = PhotoManager.base64ToBitmap(locker.photoUrl)
            if (bitmap != null) img.setImageBitmap(bitmap) else img.setImageDrawable(null)
        } else {
            img.setImageDrawable(null)
        }
        return view
    }
}

// Triple: nome, photoUrl, nomeArmario (null = não mostrar armário)
class StockAdapter(
    private val context: Context,
    private val items: List<Triple<String, String?, String?>>
) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.imgPhoto)
        val txtNome: TextView = view.findViewById(R.id.txtName)
        val txtArmario: TextView = view.findViewById(R.id.txtArmario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_locker, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (nome, photoUrl, nomeArmario) = items[position]
        holder.txtNome.text = nome
        if (nomeArmario != null) {
            holder.txtArmario.text = "Armário: $nomeArmario"
            holder.txtArmario.visibility = View.VISIBLE
        } else {
            holder.txtArmario.visibility = View.GONE
        }
        if (photoUrl != null) {
            val bitmap = PhotoManager.base64ToBitmap(photoUrl)
            if (bitmap != null) holder.imgFoto.setImageBitmap(bitmap)
            else holder.imgFoto.setImageDrawable(null)
        } else {
            holder.imgFoto.setImageDrawable(null)
        }
    }
}
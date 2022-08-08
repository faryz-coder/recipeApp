package com.faryz.recipeapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log.d
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.faryz.recipeapp.databinding.RecipeListRowV2Binding
import com.faryz.recipeapp.utils.ItemClickListener
import java.util.*

class RecipeListAdapter(
    private val recipeList: MutableList<RecipeDetails>,
    var itemClickListener: ItemClickListener,
) : RecyclerView.Adapter<RecipeListAdapter.ViewHolder>() {
    class ViewHolder(val binding: RecipeListRowV2Binding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecipeListRowV2Binding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        d("bomoh", "adapter: ${recipeList}")
        val recipeName = recipeList[position].name
        holder.binding.recipeRowName.text = recipeName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
        holder.binding.recipeRow.setOnClickListener {
            val bundle = bundleOf("recipeName" to recipeName)
            holder.binding.root.findNavController().navigate(R.id.action_FirstFragment_to_InfoFragment, bundle)
        }
        holder.binding.editButton.setOnClickListener {
            val bundle = bundleOf("recipeName" to recipeName)
            holder.binding.root.findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
        holder.binding.deleteButton.setOnClickListener {
            itemClickListener.onClick(position)
            confirmationDialog(position, holder.binding)
        }
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    fun confirmationDialog(position: Int, binding: RecipeListRowV2Binding) {
        val alertDialog: AlertDialog? = binding.root.context?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage("Confirm?")
                setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User clicked OK button
                        recipeList.removeAt(position)
                        notifyDataSetChanged()
                        dialog.dismiss()
                    })
                setNegativeButton("CANCEL",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User cancelled the dialog
                        dialog.dismiss()
                    })
            }
            builder.create()
        }
        alertDialog?.show()
    }
}



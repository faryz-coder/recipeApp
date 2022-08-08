package com.faryz.recipeapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.faryz.recipeapp.databinding.FragmentFirstBinding
import com.faryz.recipeapp.utils.ItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: FragmentFirstBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView
    private var recipeList: MutableList<RecipeDetails> = mutableListOf()
    private lateinit var sharedPref: SharedPreferences
    lateinit var itemClickListener: ItemClickListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        sharedPref = requireActivity().getPreferences(
            Context.MODE_PRIVATE
        )
        val checkFirstTimeInstall = sharedPref.getString(getString(R.string.app_name), "")
        if (checkFirstTimeInstall.isNullOrEmpty()) {
            // first time open after install or clear cache
            // add default data
            addDefaultData()
            with(sharedPref.edit()) {
                putString(
                    getString(R.string.app_name),
                    getString(R.string.app_name)
                )
                apply()
            }
        } else {
            val listRecipe = sharedPref.getString(getString(R.string.recipe), "")

            recipeList = if (!listRecipe.isNullOrEmpty()) {
                Json.decodeFromString<MutableList<RecipeDetails>>(listRecipe)
            } else {
                mutableListOf()
            }
        }


        // populate spinner with th recipe type
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.type_of_recipe,
            R.layout.list_item
        ). also { arrayAdapter ->
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            (binding.recipeTypeSpinner.editText as? AutoCompleteTextView)?.setAdapter(arrayAdapter)
        }

        // Initialize onclick listener
        itemClickListener = object : ItemClickListener {
            override fun onClick(position: Int) {
                d("bomoh", "initItem: $position")
                removeRecipe(position)
            }
        }

        // Recycler View
        recyclerView = binding.recipeListRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FirstFragment.requireContext())
            adapter = RecipeListAdapter(recipeList, itemClickListener)
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.recipeTypeSpinner.editText?.addTextChangedListener {
            val recipeTypes = it.toString()
            val filterRecipe = recipeList.filter { ty -> recipeTypes.contains(ty.type) }
            recyclerView.adapter = RecipeListAdapter(filterRecipe.toMutableList(), itemClickListener)
        }
    }


    override fun onResume() {
        super.onResume()

        refreshData()
    }

    private fun refreshData() {
        val listRecipe = sharedPref.getString(getString(R.string.recipe), "")
        recipeList = if (!listRecipe.isNullOrEmpty()) {
            Json.decodeFromString<MutableList<RecipeDetails>>(listRecipe)
        } else {
            mutableListOf()
        }
        d(tag, "recipeList: $recipeList")
        recyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val itemSelected = parent?.getItemAtPosition(position)
        Toast.makeText(context, "Selected: $itemSelected", Toast.LENGTH_SHORT).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Toast.makeText(context, "Nothing selected", Toast.LENGTH_SHORT).show()
    }

    private fun removeRecipe(position: Int) {
        val location = recipeList[position]
        // remove from sharedPref
        recipeList.removeAt(position)
        // remove img from filesDir
        File(location.imgUri).removeImg()
        // Update SharedPref
        with(sharedPref.edit()) {
            putString(
                getString(R.string.recipe),
                Json.encodeToString(recipeList)
            )
            apply()
        }
    }


    // Function for removing the specify image stored
    fun File.removeImg() : Boolean{
        return this.delete()
    }

    fun addDefaultData() {
        recipeList.add(RecipeDetails("sample", resources.getStringArray(R.array.type_of_recipe)[0], "1 cup low-fat plain or vanilla yogurt\n" +
                "⅔ cup chopped peaches (fresh, frozen, or canned and drained)\n" +
                "⅔ cup blueberries (fresh or frozen)\n" +
                "2 Tablespoons granola",

            "Wash hands with soap and water.\n" +
                    "Divide yogurt between 2 clear glasses or dishes.\n" +
                    "Spoon half of the peaches and blueberries on top of the yogurt.\n" +
                    "Sprinkle each sundae with granola.\n" +
                    "Refrigerate leftovers within 2 hours.",
            "sample_image"))
        with(sharedPref.edit()) {
            putString(
                getString(R.string.recipe),
                Json.encodeToString(recipeList)
            )
            apply()
        }
    }
}
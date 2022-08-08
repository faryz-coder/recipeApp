package com.faryz.recipeapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.faryz.recipeapp.databinding.FragmentFirstBinding
import com.faryz.recipeapp.databinding.FragmentInfoBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File


class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!
    var recipeName: String? = null
    private lateinit var sharedPref : SharedPreferences
    var recipeList = mutableListOf<RecipeDetails>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentInfoBinding.inflate(inflater, container, false)

        // Fetch the passed argument if available
        val receiveArgument = arguments?.getString("recipeName")

        if (!receiveArgument.isNullOrEmpty()) {
            recipeName = receiveArgument
        } else {
            findNavController().popBackStack()
        }

        // Initialized sharedPref
        sharedPref = requireActivity().getPreferences(
            Context.MODE_PRIVATE
        )

        // get data from sharedPref
        val getDataFromSharedPref = sharedPref.getString(getString(R.string.recipe), "")

        // decode the data to mutablelist or assign empty mutablelist if empty
        recipeList = if (getDataFromSharedPref!!.isNotEmpty()) {
            Json.decodeFromString<MutableList<RecipeDetails>>(getDataFromSharedPref)
        } else {
            mutableListOf()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selectedRecipe = recipeList.firstOrNull {it.name == recipeName}
        if (selectedRecipe?.imgUri != "sample_image") {
            val imgFile = File(selectedRecipe!!.imgUri)
            binding.infoRecipeImage.setImageURI(imgFile.toUri())
        } else {
            val res = resources.getIdentifier(selectedRecipe.imgUri, "drawable", requireContext().packageName)
            binding.infoRecipeImage.setImageResource(res)
        }

        binding.infoRecipeName.text = selectedRecipe.name
        binding.infoRecipeIngredients.text = selectedRecipe.ingredients
        binding.infoRecipeSteps.text = selectedRecipe.steps

        binding.floatingBackButtonInfo.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
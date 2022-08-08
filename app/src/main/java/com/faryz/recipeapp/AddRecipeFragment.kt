package com.faryz.recipeapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.faryz.recipeapp.databinding.FragmentAddRecipeBinding
import com.faryz.recipeapp.databinding.RecipeListRowV2Binding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class AddRecipeFragment : Fragment() {

    private var _binding: FragmentAddRecipeBinding? = null
    private val pickImage = 100
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var sharedPref : SharedPreferences
    private var selectedType: String = ""
    var imgUri: Uri? = null
    var imgFile: File? = null
    var recipeName: String? = null
    var editMode: Boolean = false
    var recipeList = mutableListOf<RecipeDetails>()
    private var recipePosition = 0
    private lateinit var spinnerAdapter : ArrayAdapter<CharSequence>
    private var inputIngredient = ""
    private var inputSteps = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAddRecipeBinding.inflate(inflater, container, false)

        // Fetch the passed argument if available
        val receiveArgument = arguments?.getString("recipeName")

        // check for the arguments status
        // null = editmode off
        // notNull = editmode on
        if (!receiveArgument.isNullOrEmpty()) {
            recipeName = receiveArgument
            editMode = true
        } else {
            editMode = false
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

        d(tag, "GetList:: $recipeList")
        d(tag, "recipeName: $receiveArgument")

        // Selected Image - Activity Result
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                d(tag, "image selected")
                imgUri = it.data?.data
                requireActivity().contentResolver.takePersistableUriPermission(imgUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                binding.addImageView.setImageURI(imgUri)
                binding.selectImage.setText("re-select image?")
//                requireActivity().contentResolver.takePersistableUriPermission(imgUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                d(tag, "image not selected")
            }
        }

        // populate spinner with th recipe type
        spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.type_of_recipe,
            R.layout.list_item,
        )

        (binding.recipeTypesSpinner.editText as? AutoCompleteTextView)?.setAdapter(spinnerAdapter)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check for edit mode status
        // redisplayed the existing data if edit mode ON
        if (editMode) {
            // Display Current Recipe
            val selectedRecipe = recipeList.firstOrNull {it.name == recipeName}
            imgFile = File(selectedRecipe?.imgUri.toString())
            selectedType = selectedRecipe?.type.toString()
            binding.recipeName.editText?.setText(selectedRecipe?.name)
            binding.recipeIngredients.editText?.setText(selectedRecipe?.ingredients)
            (binding.recipeTypesSpinner.editText as? AutoCompleteTextView)?.setText(selectedRecipe?.type, false)
            binding.recipeSteps.editText?.setText(selectedRecipe?.steps)
            if (selectedRecipe?.imgUri == "sample_image") {
                val res = resources.getIdentifier(selectedRecipe.imgUri, "drawable", requireContext().packageName)
                binding.addImageView.setImageResource(res)
            } else {
                binding.addImageView.setImageURI(imgFile!!.toUri())
                imgUri = imgFile!!.toUri()
            }
            binding.submitButton.text = "UPDATE"

            recipePosition = recipeList.indexOf(selectedRecipe)
        }

        binding.selectImage.setOnClickListener {
            selectImageFromGallery()
        }
        binding.submitButton.setOnClickListener {
            // Validate if the form filled properly
            if (validate()) {
                addRecipe()
            } else {
                Toast.makeText(requireContext(), "Incomplete Form", Toast.LENGTH_SHORT).show()
            }
        }

        binding.addRecipeLayout.setOnClickListener{
            closeKeyBoard(it)
        }
        binding.scrollViewLayout.setOnClickListener {
            closeKeyBoard(it)
        }
        binding.linearLayout.setOnClickListener {
            closeKeyBoard(it)
        }
        binding.recipeTypesSpinner.editText?.addTextChangedListener {
            if (selectedType != it.toString()) {
                selectedType = it.toString()
            } else { selectedType }
        }
        binding.floatingBackButtonInfo3.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun validate(): Boolean {
        val selectedRecipe = recipeList.firstOrNull {it.name == recipeName}
        val name = binding.recipeName.editText?.text.toString()
        val ingredient = binding.recipeIngredients.editText?.text.toString()
        val steps = binding.recipeSteps.editText?.text.toString()
        return (imgUri != null || selectedRecipe?.imgUri == "sample_image") && ingredient.isNotEmpty() && steps.isNotEmpty() && name.isNotEmpty() && selectedType.isNotEmpty()
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT , MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun addRecipe() {
        val name = binding.recipeName.editText?.text.toString()
        val ingredient = binding.recipeIngredients.editText?.text!!.reformatText()
        val steps = binding.recipeSteps.editText?.text.toString()

        // Get Image Bitmap
        val imgBitmap = if (android.os.Build.VERSION.SDK_INT >= 29) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireActivity().contentResolver, imgUri!!))
        } else {
            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imgUri)
        }

        // Save image into internal storage
        val imgFile = requireContext().saveImg(name, imgBitmap)

        // Display Image
        binding.addImageView.setImageURI(imgFile.toUri())

        if (editMode) {
            val selectedRecipe = recipeList.firstOrNull {it.name == recipeName}
            val oldUri = File(selectedRecipe?.imgUri.toString())
            // remove outdated image in the fileDir
            oldUri.removeImg()
            // remove the recipe from the list
            recipeList.removeAt(recipePosition)
            // add the updated recipe to the list
            recipeList.add(RecipeDetails(name, selectedType, ingredient, steps, imgFile.toString()))
        } else {
            // add the new recipe to the list
            recipeList.add(RecipeDetails(name, selectedType, ingredient, steps, imgFile.toString()))
        }

        // Save the list in the sharedPref by encode the list to jsonString
        with(sharedPref.edit()) {
            putString(
                getString(R.string.recipe),
                Json.encodeToString(recipeList)
            )
            apply()
        }

        completeDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Function for saving the img
    // [recipeName] : name of the recipe
    // [bitmap] : bitmap of the image
    fun Context.saveImg(recipeName: String, bitmap: Bitmap): File {
        val filesDir = requireActivity().filesDir
        val fileName = "${recipeName}_${UUID.randomUUID()}"
        val file = File(filesDir, fileName)
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    }

    // Function for removing the specify image stored
    fun File.removeImg() : Boolean{
        return this.delete()
    }

    private fun closeKeyBoard(v : View) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    // Function for formatting the text so that when display in textview it will include the newline
    private fun Editable.reformatText(): String {
        var formattedString = ""
        for (i in this) {
            if( -1 != i.toString().indexOf("\n") ){
                // contain newline
                d(tag, "newline")
                formattedString += "\n"
            } else {
                // normal text
                formattedString += i
            }
        }
        return formattedString
    }

    private fun completeDialog() {
        val alertDialog: AlertDialog? = binding.root.context?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage("Proceed?")
                setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User clicked OK button
                        findNavController().popBackStack()
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


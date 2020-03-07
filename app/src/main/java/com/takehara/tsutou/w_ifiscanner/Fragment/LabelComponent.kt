package com.takehara.tsutou.w_ifiscanner.Fragment

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.takehara.tsutou.w_ifiscanner.Activity.LabelActivity
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.fragment_label_component.*
import kotlinx.android.synthetic.main.fragment_label_component.view.*
import org.angmarch.views.NiceSpinner



class LabelComponent : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_label_component, container, false)

        // Building spinner
        val building_types = mutableListOf<String>("綜合科館", "共同科館")
        val building_spinner = view.building_spinner as NiceSpinner
        building_spinner.setTextColor(Color.BLACK)
        building_spinner.attachDataSource(building_types)

        // Floor spinner
        val floor_types = mutableListOf<String>("1F", "2F")
        val floor_spinner = view.floor_spinner as NiceSpinner
        floor_spinner.setTextColor(Color.BLACK)
        floor_spinner.attachDataSource(floor_types)

        // Classroom spinner
        val classroom_types = mutableListOf<String>("107-1", "109-1")
        val classroom_spinner = view.classroom_spinner as NiceSpinner
        classroom_spinner.setTextColor(Color.BLACK)
        classroom_spinner.attachDataSource(classroom_types)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        view.label_btn.setOnClickListener {

            val intent = Intent(activity, LabelActivity::class.java)

            intent.putExtra("building", building_spinner.text.toString())
            intent.putExtra("floor", floor_spinner.text.toString())
            intent.putExtra("classroom", classroom_spinner.text.toString())
            startActivity(intent)
        }

    }

}

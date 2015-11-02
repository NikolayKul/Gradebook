package com.nikolaykul.gradebook.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nikolaykul.gradebook.R;
import com.nikolaykul.gradebook.adapters.StudentListViewHolder;
import com.nikolaykul.gradebook.data.local.Database;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;
import uk.co.ribot.easyadapter.EasyRecyclerAdapter;

public class StudentListFragment extends BaseFragment {
    @Bind(R.id.student_list) RecyclerView mRecyclerView;
    @Inject Context mContext;
    @Inject Database mDatabase;
    private StudentListViewHolder.StudentListener mListener = student -> {
        Timber.i("student:\nid = %d,\nname = %s", student.id, student.fullName);
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivityComponent().inject(this);
        populateList();
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    private void populateList() {
        mRecyclerView.setAdapter(new EasyRecyclerAdapter<>(
                mContext,
                StudentListViewHolder.class,
                mDatabase.getStudents(),
                mListener));
    }

}
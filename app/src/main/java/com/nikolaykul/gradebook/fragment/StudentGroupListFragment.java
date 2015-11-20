package com.nikolaykul.gradebook.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nikolaykul.gradebook.R;
import com.nikolaykul.gradebook.adapter.GroupViewHolder;
import com.nikolaykul.gradebook.event.FloatingActionButtonEvent;
import com.nikolaykul.gradebook.data.local.Database;
import com.nikolaykul.gradebook.data.model.StudentGroup;
import com.nikolaykul.gradebook.other.DialogFactory;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator;
import jp.wasabeef.recyclerview.animators.adapters.SlideInRightAnimationAdapter;
import uk.co.ribot.easyadapter.EasyRecyclerAdapter;

public class StudentGroupListFragment extends BaseFragment {
    private static final String BUNDLE_TAB_NUM = "tabNum";
    @Bind(R.id.recycleView) RecyclerView mRecyclerView;
    @Inject Activity mActivity;
    @Inject Database mDatabase;
    @Inject Bus mBus;
    private List<StudentGroup> mGroups;
    private int mTabNum;

    public static StudentGroupListFragment newInstance(int tabNum) {
        StudentGroupListFragment fragment = new StudentGroupListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_TAB_NUM, tabNum);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void setActivityComponent() {
        getActivityComponent().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBus.register(this);
        mGroups = mDatabase.getStudentGroups();

        Bundle args = getArguments();
        mTabNum = null != args ? args.getInt(BUNDLE_TAB_NUM) : 0;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_group_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSwipeToDelete();
        populateList();
    }

    @Override
    public void onDestroy() {
        mBus.unregister(this);
        ButterKnife.unbind(this);
        super.onDestroy();
    }

    @Subscribe public void showNewGroupDialog(FloatingActionButtonEvent event) {
        if (mTabNum != event.currentTabNum) return;

        DialogFactory.getMaterialAddDialog(mActivity, StudentGroup.class,
                (materialDialog, dialogAction) -> {
                    materialDialog.dismiss();
                    if (null != materialDialog.getInputEditText()) {
                        String name = materialDialog.getInputEditText().getText().toString();
                        if (!name.isEmpty()) {
                            // create group
                            StudentGroup newGroup = new StudentGroup(name);
                            // insert
                            mDatabase.insertStudentGroup(newGroup);
                            addGroup(newGroup, mGroups.size());
                            Toast.makeText(mActivity,
                                    R.string.dialog_add_studentGroup_success,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void populateList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        EasyRecyclerAdapter adapter = new EasyRecyclerAdapter<>(
                mActivity,
                GroupViewHolder.class,
                mGroups,
                (GroupViewHolder.StudentGroupListener) mBus::post);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new SlideInRightAnimator());
        mRecyclerView.setAdapter(new SlideInRightAnimationAdapter(adapter));
    }

    private void addGroup(StudentGroup group, int position) {
        mGroups.add(position, group);
        mRecyclerView.getAdapter().notifyItemInserted(position);
    }

    private void removeGroup(int position) {
        mGroups.remove(position);
        mRecyclerView.getAdapter().notifyItemRemoved(position);
    }

    private void setSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        final int deletedPosition = viewHolder.getAdapterPosition();
                        final StudentGroup deletedGroup = mGroups.get(deletedPosition);

                        // delete student from list
                        removeGroup(deletedPosition);

                        // show Snackbar
                        View focusedView = mActivity.getCurrentFocus();
                        if (null == focusedView) focusedView = mRecyclerView;

                        String message =
                                getResources().getString(R.string.message_delete_studentGroup_successful);
                        message = String.format(message, deletedGroup.name);
                        Snackbar.make(focusedView, message, Snackbar.LENGTH_LONG)
                                .setCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        super.onDismissed(snackbar, event);
                                        // if "undo" wasn't called -> delete from database
                                        switch (event) {
                                            case Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE:
                                            case Snackbar.Callback.DISMISS_EVENT_SWIPE:
                                            case Snackbar.Callback.DISMISS_EVENT_TIMEOUT:
                                                mDatabase.removeStudentGroup(deletedGroup.id);
                                        }
                                    }
                                })
                                .setActionTextColor(
                                        ContextCompat.getColor(mActivity, R.color.purple_light))
                                .setAction(R.string.action_undo, iView -> {
                                    // if "undo" was called -> restore student in list
                                    addGroup(deletedGroup, deletedPosition);
                                })
                                .show();
                    }
                };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

}
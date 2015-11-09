package com.nikolaykul.gradebook.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nikolaykul.gradebook.R;
import com.nikolaykul.gradebook.data.local.Database;
import com.nikolaykul.gradebook.data.models.StudentGroup;
import com.nikolaykul.gradebook.utils.KeyboardUtil;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

public class StudentGroupFragment extends BaseFragment {
    @Bind(R.id.gridLayout) GridLayout mGridLayout;
    @Inject Context mContext;
    @Inject Database mDatabase;
    private float mGroupsTextSize;
    private List<StudentGroup> mGroups;
    private AlertDialog mNewGroupDialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getActivityComponent().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mGroups = mDatabase.getStudentGroups();
        mGroupsTextSize = getResources().getDimension(R.dimen.student_group_text_size);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_group, container, false);
        ButterKnife.bind(this, view);
        refreshContainer();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_student_group, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: showNewGroupDialog(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        if (null != mNewGroupDialog) mNewGroupDialog.dismiss();
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    private void refreshContainer() {
        mGridLayout.removeAllViews();
        for (StudentGroup group : mGroups) {
            mGridLayout.addView(createViewContent(group));
        }
    }

    private TextView createViewContent(StudentGroup group) {
        LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(
                0,
                (int) getResources().getDimension(R.dimen.student_group_layout_height));
        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams(linearParams);
        gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1.0f);

        TextView tv = new TextView(mContext);
        tv.setLayoutParams(gridParams);
        tv.setSingleLine(true);
        tv.setGravity(Gravity.CENTER);
        tv.setText(group.name);
        tv.setTextSize(mGroupsTextSize);
        tv.setTextColor(ContextCompat.getColor(mContext, android.R.color.black));

        // setBackground
        Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.circle_gradient);
        if (Build.VERSION.SDK_INT < 16) {
            tv.setBackgroundDrawable(drawable);
        } else {
            tv.setBackground(drawable);
        }

        tv.setTag(group);
        tv.setOnClickListener(iView -> {
            StudentGroup currentGroup = (StudentGroup) tv.getTag();
            // TODO:
            Timber.i("OnClick -> Group: id = %d, name = %s", currentGroup.id, currentGroup.name);
        });
        tv.setOnLongClickListener(iView -> {
            StudentGroup currentGroup = (StudentGroup) tv.getTag();
            // TODO:
            Timber.i("OnLongClick -> Group: id = %d, name = %s", currentGroup.id, currentGroup.name);
            return true;
        });

        return tv;
    }

    private void showNewGroupDialog() {
        mNewGroupDialog = new AlertDialog.Builder(getActivity())
                .setView(createViewForDialog())
                .create();
        mNewGroupDialog.show();
    }

    private View createViewForDialog() {
        View layout =
                getActivity().getLayoutInflater().inflate(R.layout.dialog_add_student_group, null);
        EditText etGroupName = (EditText) layout.findViewById(R.id.group_name);
        FloatingActionButton fab = (FloatingActionButton) layout.findViewById(R.id.fab);

        etGroupName.postDelayed(() -> KeyboardUtil.showKeyboard(mContext), 50);
        fab.setOnClickListener(iView -> {
            String groupName = etGroupName.getText().toString();
            if (!groupName.isEmpty()) {
                // create group
                StudentGroup newGroup = new StudentGroup(groupName);
                // insert
                newGroup.id = mDatabase.insertStudentGroup(newGroup);
                mGroups.add(newGroup);
                refreshContainer();
                Toast.makeText(mContext,
                        R.string.dialog_add_student_group_success,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext,
                        R.string.dialog_add_student_group_error,
                        Toast.LENGTH_SHORT).show();
            }
            KeyboardUtil.hideKeyboard(getActivity());
            if (null != mNewGroupDialog) mNewGroupDialog.dismiss();
            fab.setEnabled(true);
        });

        return layout;
    }

}
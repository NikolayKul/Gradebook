package com.nikolaykul.gradebook.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.nikolaykul.gradebook.R;
import com.nikolaykul.gradebook.data.local.Database;
import com.nikolaykul.gradebook.data.models.Student;
import com.nikolaykul.gradebook.data.models.StudentInfo;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StudentInfoFragment extends BaseFragment {
    private static final String BUNDLE_INFO_TABLE = "infoTable";
    @Bind(R.id.table) TableLayout mTable;
    @Bind(R.id.students_column) LinearLayout mColumnStudents;
    @Inject Context mContext;
    @Inject Database mDatabase;
    private short mStudentInfoTable;
    private List<Student> mStudents;
    private float mStudentsTextSize;
    private int mRowViewHeight;
    private int mRowViewWidth;
    private AlertDialog mNewStudentInfoDialog;
    private AlertDialog mDeleteStudentInfoDialog;

    public static StudentInfoFragment getInstance(short infoTable) {
        StudentInfoFragment fragment = new StudentInfoFragment();
        Bundle bundle = new Bundle();
        bundle.putShort(BUNDLE_INFO_TABLE, infoTable);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getActivityComponent().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mStudentInfoTable = savedInstanceState.getByte(BUNDLE_INFO_TABLE);
        } else {
            mStudentInfoTable = Database.STUDENT_ATTENDANCE;
        }
        mStudents = mDatabase.getStudents();
        mRowViewWidth = (int) getResources().getDimension(R.dimen.table_row_view_width);
        mRowViewHeight = (int) getResources().getDimension(R.dimen.table_row_view_height);
        mStudentsTextSize = getResources().getDimension(R.dimen.table_students_text_size);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_info, container, false);
        ButterKnife.bind(this, view);
        refreshContainers();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_student_info, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: showNewStudentInfoDialog(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        if (null != mNewStudentInfoDialog) mNewStudentInfoDialog.dismiss();
        if (null != mDeleteStudentInfoDialog) mDeleteStudentInfoDialog.dismiss();
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    private void showNewStudentInfoDialog() {
        mNewStudentInfoDialog = new AlertDialog.Builder(getActivity())
                .setView(createViewForDialogAdd())
                .create();
        mNewStudentInfoDialog.show();
    }

    private void showDeleteInfoDialog(StudentInfo info) {
        mDeleteStudentInfoDialog = new AlertDialog.Builder(getActivity())
                .setView(createViewForDialogDelete(info))
                .create();
        mDeleteStudentInfoDialog.show();
    }

    private void refreshContainers() {
        // clear
        mColumnStudents.removeAllViews();
        mTable.removeAllViews();

        if (mStudents.isEmpty()) return;

        // populate
        populateHeader();
        for (Student student : mStudents) {
            // populate LinearLayout (students)
            mColumnStudents.addView(createViewStudentName(student.fullName));

            // populate TableLayout (content)
            mTable.addView(createRowContent(student.id));
            mTable.addView(createDivider(true));
        }
    }

    private void populateHeader() {
        mColumnStudents.addView(createViewEmpty());
        mTable.addView(createRowHeader());
    }

    private TableRow createRowHeader() {
        TableRow row = new TableRow(mContext);

        long someStudentId = mStudents.get(0).id;
        List<StudentInfo> infoList = mDatabase.getStudentInfo(someStudentId, mStudentInfoTable);
        for (StudentInfo info : infoList) {
            row.addView(createViewHeader(info));
            row.addView(createDivider(false));
        }
        return row;
    }

    private TableRow createRowContent(long studentId) {
        TableRow row = new TableRow(mContext);
        List<StudentInfo> infoList = mDatabase.getStudentInfo(studentId, mStudentInfoTable);
        for (StudentInfo info : infoList) {
            row.addView(createViewContent(info));
            row.addView(createDivider(false));
        }
        return row;
    }

    private TextView createViewStudentName(String studentName) {
        TextView tv = new TextView(mContext);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mRowViewHeight));
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine();
        tv.setTextSize(mStudentsTextSize);
        tv.setText(studentName);
        return tv;
    }

    private TextView createViewHeader(StudentInfo info) {
        final DateFormat df = new SimpleDateFormat("dd/MM", Locale.getDefault());
        TextView tv = new TextView(mContext);
        tv.setLayoutParams(new TableRow.LayoutParams(mRowViewWidth, mRowViewHeight));
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine();
        tv.setTextSize(mStudentsTextSize);
        tv.setText(df.format(info.date));
        tv.setTag(info);
        tv.setOnLongClickListener(iView -> {
            StudentInfo currentInfo = (StudentInfo) tv.getTag();
            showDeleteInfoDialog(currentInfo);
            return true;
        });
        return tv;
    }

    private View createViewContent(StudentInfo info) {
        View view = new View(mContext);
        view.setLayoutParams(new TableRow.LayoutParams(mRowViewWidth, mRowViewHeight));
        view.setBackgroundColor(ContextCompat.getColor(mContext, info.wasGood
                ? R.color.green
                : R.color.red));
        view.setTag(info);
        view.setOnClickListener(iView -> {
            StudentInfo currentInfo = (StudentInfo) view.getTag();
            currentInfo.wasGood = !currentInfo.wasGood;
            view.setBackgroundColor(ContextCompat.getColor(mContext, currentInfo.wasGood
                    ? R.color.green
                    : R.color.red));
            mDatabase.updateStudentInfo(currentInfo, mStudentInfoTable);
            view.setTag(currentInfo);
        });
        return view;
    }

    private View createViewEmpty() {
        View view = new View(mContext);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mRowViewHeight));
        return view;
    }

    /**
     * Creates whether horizontal (for rows) or vertical (for columns) divider.
     * @return view that represents a divider.
     */
    private View createDivider(boolean isHorizontalDivider) {
        int width  = isHorizontalDivider ? TableRow.LayoutParams.MATCH_PARENT : 1;
        int height = isHorizontalDivider ? 1 : TableRow.LayoutParams.MATCH_PARENT;

        View view = new View(mContext);
        view.setLayoutParams(new TableRow.LayoutParams(width, height));
        view.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        return view;
    }

    private View createViewForDialogAdd() {
        View layout =
                getActivity().getLayoutInflater().inflate(R.layout.dialog_add_student_info, null);
        MaterialCalendarView calendarView =
                (MaterialCalendarView) layout.findViewById(R.id.calendarView);
        FloatingActionButton fab =
                (FloatingActionButton) layout.findViewById(R.id.fab);

        calendarView.clearSelection();
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_MULTIPLE);
        fab.setOnClickListener(iView -> {
            fab.setEnabled(false);
            List<CalendarDay> calendarDayList = calendarView.getSelectedDates();
            if (!calendarDayList.isEmpty()) {
                for (CalendarDay calendarDay : calendarDayList) {
                    mDatabase.insertStudentInfo(calendarDay.getDate(), mStudentInfoTable);
                }
                refreshContainers();
            }
            if (null != mNewStudentInfoDialog) mNewStudentInfoDialog.dismiss();
            fab.setEnabled(true);
        });

        return layout;
    }

    private View createViewForDialogDelete(StudentInfo info) {
        final DateFormat df = new SimpleDateFormat("dd/MM", Locale.getDefault());
        View layout =
                getActivity().getLayoutInflater().inflate(R.layout.dialog_delete_student_info, null);
        TextView tvMessage = (TextView) layout.findViewById(R.id.message);
        ImageButton btnOk = (ImageButton) layout.findViewById(R.id.ok);
        ImageButton btnNo = (ImageButton) layout.findViewById(R.id.no);

        String message = getResources().getString(R.string.dialog_delete_student_info_message);
        tvMessage.setText(String.format(message, df.format(info.date)));
        btnOk.setOnClickListener(iView -> {
            mDatabase.removeStudentInfo(info.date, mStudentInfoTable);
            refreshContainers();
            if (null != mDeleteStudentInfoDialog) mDeleteStudentInfoDialog.dismiss();
        });
        btnNo.setOnClickListener(iView -> {
            if (null != mDeleteStudentInfoDialog) mDeleteStudentInfoDialog.dismiss();
        });

        return layout;
    }

}

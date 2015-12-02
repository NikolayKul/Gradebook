package com.nikolaykul.gradebook.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.nikolaykul.gradebook.R;
import com.nikolaykul.gradebook.data.local.Database;
import com.nikolaykul.gradebook.data.model.Group;
import com.nikolaykul.gradebook.data.model.Information;
import com.nikolaykul.gradebook.data.model.Student;
import com.nikolaykul.gradebook.event.FloatingActionButtonEvent;
import com.nikolaykul.gradebook.event.StudentAddedEvent;
import com.nikolaykul.gradebook.event.StudentDeletedEvent;
import com.nikolaykul.gradebook.other.DialogFactory;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

@SuppressWarnings("unused")
public class StudentInfoFragment extends BaseFragment {
    private static final String BUNDLE_TAB_NUM = "tabNum";
    private static final String BUNDLE_INFO_TABLE = "infoTable";
    private static final String BUNDLE_GROUP = "group";
    @Bind(R.id.table) TableLayout mTable;
    @Bind(R.id.students_layout) LinearLayout mColumnStudents;
    @Bind(R.id.header_layout) LinearLayout mHeaderLayout;
    @Bind(R.id.scroll_students_layout) NestedScrollView mScrollStudentsColumn;
    @Bind(R.id.scroll_table) NestedScrollView mScrollTable;
    @Inject Activity mActivity;
    @Inject Database mDatabase;
    @Inject Bus mBus;
    private List<Student> mStudents;
    private int mTabNum;
    private int mInfoTable;
    private long mGroupId;
    // dimens
    private float mStudentsTextSize;
    private float mDateTextSize;
    private int mStudentsTextPadding;
    private int mRowViewHeight;
    private int mRowViewWidth;

    public static StudentInfoFragment newInstance(int tabNum,
                                                  @Database.InformationTable int infoTable,
                                                  long groupId) {
        StudentInfoFragment fragment = new StudentInfoFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_TAB_NUM, tabNum);
        bundle.putInt(BUNDLE_INFO_TABLE, infoTable);
        bundle.putLong(BUNDLE_GROUP, groupId);
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

        Bundle args = getArguments();
        if (null != args) {
            mTabNum = args.getInt(BUNDLE_TAB_NUM);
            mInfoTable = args.getInt(BUNDLE_INFO_TABLE);
            mGroupId = args.getLong(BUNDLE_GROUP);
        } else {
            mTabNum = 0;
            mInfoTable = Database.TABLE_ATTENDANCE;
            mGroupId = -1;
        }
        mStudents = mDatabase.getStudents(mGroupId);
        mStudentsTextSize = getResources().getDimension(R.dimen.text_small_size);
        mStudentsTextPadding = (int) getResources().getDimension(R.dimen.text_normal_padding);
        mDateTextSize = getResources().getDimension(R.dimen.text_tiny_size);
        mRowViewWidth = (int) getResources().getDimension(R.dimen.table_row_view_width);
        mRowViewHeight = (int) getResources().getDimension(R.dimen.table_row_view_height);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_info, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setScrollListener();
        refreshContainers();
    }

    @Override
    public void onDestroy() {
        mBus.unregister(this);
        ButterKnife.unbind(this);
        super.onDestroy();
    }

    @Subscribe public void showNewStudentInfoDialog(FloatingActionButtonEvent event) {
        if (mTabNum != event.currentTabNum) return;

        DialogFactory.getMaterialAddDialog(mActivity, Information.class,
                (materialDialog, dialogAction) -> {
                    materialDialog.dismiss();
                    MaterialCalendarView calendarView =
                            (MaterialCalendarView) materialDialog.getCustomView();
                    if (null == calendarView) return;

                    List<CalendarDay> calendarDayList = calendarView.getSelectedDates();
                    if (calendarDayList.isEmpty()) return;

                    Information newInformation = new Information()
                            .setTitle("")
                            .setContent("");
                    for (CalendarDay calendarDay : calendarDayList) {
                        newInformation.setDate(new DateTime(calendarDay.getDate()));
                        mDatabase.insertInformation(newInformation, mGroupId, mInfoTable);
                    }
                    refreshContainers();
                })
                .show();
    }

    @Subscribe public void onGroupSelected(Group group) {
        mGroupId = group.getId();
        mStudents.clear();
        mStudents.addAll(mDatabase.getStudents(mGroupId));
        refreshContainers();
    }

    @Subscribe public void onStudentAdded(StudentAddedEvent event) {
        mStudents.clear();
        mStudents.addAll(mDatabase.getStudents(mGroupId));
        refreshContainers();
    }

    @Subscribe public void onStudentDeleted(StudentDeletedEvent event) {
        mStudents.clear();
        mStudents.addAll(mDatabase.getStudents(mGroupId));
        refreshContainers();
    }

    private void showDeleteInfoDialog(Information info) {
        DialogFactory.getMaterialDeleteDialog(mActivity, info,
                (materialDialog, dialogAction) -> {
                    materialDialog.dismiss();
                    mDatabase.removeInformation(info, mGroupId, mInfoTable);
                    refreshContainers();
                })
                .show();
    }

    /**
     * Enable scrolling {@link #mScrollStudentsColumn} and {@link #mScrollTable} simultaneously.
     */
    private void setScrollListener() {
        NestedScrollView.OnScrollChangeListener scrollChangeListener =
                (NestedScrollView view, int x, int y, int oldX, int oldY) -> {
                    if (view == mScrollStudentsColumn) {
                        mScrollTable.scrollTo(x, y);
                    } else {
                        mScrollStudentsColumn.scrollTo(x, y);
                    }
                };
        mScrollTable.setOnScrollChangeListener(scrollChangeListener);
        mScrollStudentsColumn.setOnScrollChangeListener(scrollChangeListener);
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
            mColumnStudents.addView(createViewStudentName(student.getFullName()));

            // populate TableLayout (content)
            mTable.addView(createRowContent(student.getId()));
            mTable.addView(createDivider(true));
        }
    }

    private void populateHeader() {
        mColumnStudents.addView(createViewEmpty());

        long someStudentId = mStudents.get(0).getId();
        List<Information> infoList = mDatabase.getInformation(someStudentId, mInfoTable);
        for (Information info : infoList) {
            mHeaderLayout.addView(createViewHeader(info));
        }
    }

    private TableRow createRowContent(long studentId) {
        TableRow row = new TableRow(mActivity);
        List<Information> infoList = mDatabase.getInformation(studentId, mInfoTable);
        for (Information info : infoList) {
            row.addView(createViewContent(info));
            row.addView(createDivider(false));
        }
        return row;
    }

    private TextView createViewStudentName(String studentName) {
        TextView tv = new TextView(mActivity);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mRowViewHeight));
        tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tv.setPadding(mStudentsTextPadding, 0, 0, 0);
        tv.setSingleLine();
        tv.setTextSize(mStudentsTextSize);
        tv.setText(studentName);
        return tv;
    }

    private TextView createViewHeader(Information info) {
        final String text = info.getDate().monthOfYear().getAsShortText() +
                "/" + info.getDate().dayOfWeek().getAsShortText();
        TextView tv = new TextView(mActivity);
        tv.setLayoutParams(new TableRow.LayoutParams(mRowViewWidth, mRowViewHeight));
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine();
        tv.setTextSize(mDateTextSize);
        tv.setText(text);
        tv.setTag(info);
        tv.setOnLongClickListener(iView -> {
            Information currentInfo = (Information) tv.getTag();
            showDeleteInfoDialog(currentInfo);
            return true;
        });
        return tv;
    }

    private View createViewContent(Information info) {
        View view = new View(mActivity);
        view.setLayoutParams(new TableRow.LayoutParams(mRowViewWidth, mRowViewHeight));
        view.setBackgroundColor(ContextCompat.getColor(mActivity, info.isPassed()
                ? R.color.green
                : R.color.red));
        view.setTag(info);
        view.setOnClickListener(iView -> {
            Information currentInfo = (Information) view.getTag();
            currentInfo.setPassed(!currentInfo.isPassed());
            view.setBackgroundColor(ContextCompat.getColor(mActivity, currentInfo.isPassed()
                    ? R.color.green
                    : R.color.red));
            mDatabase.updateInformation(currentInfo, mInfoTable);
            view.setTag(currentInfo);
        });
        return view;
    }

    private View createViewEmpty() {
        View view = new View(mActivity);
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

        View view = new View(mActivity);
        view.setLayoutParams(new TableRow.LayoutParams(width, height));
        view.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.gray));
        return view;
    }

}

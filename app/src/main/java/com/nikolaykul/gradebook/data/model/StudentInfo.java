package com.nikolaykul.gradebook.data.model;

import org.joda.time.DateTime;

public class StudentInfo {
    private long mId;
    private long mStudentId;
    private DateTime mDate;
    private boolean wasGood;

    public StudentInfo(long studentId, DateTime date, boolean wasGood) {
        this.mStudentId = studentId;
        this.mDate = date;
        this.wasGood = wasGood;
    }

    public long getId() {
        return mId;
    }

    public StudentInfo setId(long id) {
        this.mId = id;
        return this;
    }

    public long getStudentId() {
        return mStudentId;
    }

    public StudentInfo setStudentId(long studentId) {
        this.mStudentId = studentId;
        return this;
    }

    public DateTime getDate() {
        return mDate;
    }

    public StudentInfo setDate(DateTime date) {
        this.mDate = date;
        return this;
    }

    public boolean isWasGood() {
        return wasGood;
    }

    public StudentInfo setWasGood(boolean wasGood) {
        this.wasGood = wasGood;
        return this;
    }

}

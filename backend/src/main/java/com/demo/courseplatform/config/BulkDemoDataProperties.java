package com.demo.courseplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "demo.bulk-seed")
public class BulkDemoDataProperties {

    private boolean enabled = false;
    private boolean resetBeforeSeed = true;
    private long randomSeed = 20260317L;
    private int courseCount = 15;
    private int membersPerCourse = 20;
    private int assignmentCount = 48;
    private int reviewingAssignmentCount = 20;
    private int closedAssignmentCount = 10;
    private int groupStudentReviewCount = 10;
    private int individualStudentReviewCount = 9;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isResetBeforeSeed() {
        return resetBeforeSeed;
    }

    public void setResetBeforeSeed(boolean resetBeforeSeed) {
        this.resetBeforeSeed = resetBeforeSeed;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public int getCourseCount() {
        return courseCount;
    }

    public void setCourseCount(int courseCount) {
        this.courseCount = courseCount;
    }

    public int getMembersPerCourse() {
        return membersPerCourse;
    }

    public void setMembersPerCourse(int membersPerCourse) {
        this.membersPerCourse = membersPerCourse;
    }

    public int getAssignmentCount() {
        return assignmentCount;
    }

    public void setAssignmentCount(int assignmentCount) {
        this.assignmentCount = assignmentCount;
    }

    public int getReviewingAssignmentCount() {
        return reviewingAssignmentCount;
    }

    public void setReviewingAssignmentCount(int reviewingAssignmentCount) {
        this.reviewingAssignmentCount = reviewingAssignmentCount;
    }

    public int getClosedAssignmentCount() {
        return closedAssignmentCount;
    }

    public void setClosedAssignmentCount(int closedAssignmentCount) {
        this.closedAssignmentCount = closedAssignmentCount;
    }

    public int getGroupStudentReviewCount() {
        return groupStudentReviewCount;
    }

    public void setGroupStudentReviewCount(int groupStudentReviewCount) {
        this.groupStudentReviewCount = groupStudentReviewCount;
    }

    public int getIndividualStudentReviewCount() {
        return individualStudentReviewCount;
    }

    public void setIndividualStudentReviewCount(int individualStudentReviewCount) {
        this.individualStudentReviewCount = individualStudentReviewCount;
    }
}

package com.ash.projects.redisclone.model;

import java.io.Serializable;

// Sorted Set Entry
public class SortedSetEntry implements Comparable<SortedSetEntry>, Serializable {
    private String member;
    private double score;

    public SortedSetEntry() {
    }

    public SortedSetEntry(String member, double score) {
        this.member = member;
        this.score = score;
    }

    @Override
    public int compareTo(SortedSetEntry other) {
        int scoreCompare = Double.compare(this.score, other.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        return this.member.compareTo(other.member);
    }

    // Getters and Setters
    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}

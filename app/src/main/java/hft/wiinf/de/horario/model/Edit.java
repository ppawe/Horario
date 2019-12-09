package hft.wiinf.de.horario.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.Date;

@Table(name = "edit")
public class Edit extends Model {
    @Column
    private String description;
    @Column
    private String place;
    @Column
    private Date startTime;
    @Column
    private Date endTime;
    @Column
    private Repetition repetition;
    @Column
    private Date endOfRepetition;
    @Column
    private boolean editFollowup;


    public Edit() {
        super();
    }

    public Edit(String description, String place, Date startTime, Date endTime, Repetition repetition, Date endOfRepetition, boolean editFollowup) {
        this.description = description;
        this.place = place;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repetition = repetition;
        this.endOfRepetition = endOfRepetition;
        this.editFollowup = editFollowup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Repetition getRepetition() {
        return repetition;
    }

    public void setRepetition(Repetition repetition) {
        this.repetition = repetition;
    }

    public Date getEndOfRepetition() {
        return endOfRepetition;
    }

    public void setEndOfRepetition(Date endOfRepetition) {
        this.endOfRepetition = endOfRepetition;
    }

    public boolean isEditFollowup() {
        return editFollowup;
    }

    public void setEditFollowup(boolean editFollowup) {
        this.editFollowup = editFollowup;
    }
}

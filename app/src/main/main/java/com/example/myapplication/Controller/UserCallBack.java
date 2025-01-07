package com.example.myapplication.Controller;

import com.example.myapplication.Model.User;
import com.google.android.gms.tasks.Task;

public interface UserCallBack {
    void onUserFetchDataComplete(User user);

    void onUpdateComplete(Task<Void> task);

}
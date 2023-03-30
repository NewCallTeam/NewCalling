/*
 * Copyright (c) 2022 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the XXXX License, Version X.X (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://xxxxxxx/licenses/LICENSE-X.X
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib.adapter.ar.local;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ARActivityUtil {

    private static final ARActivityUtil INSTANCE = new ARActivityUtil();

    private List<Activity> activities = new ArrayList<>();

    public static ARActivityUtil getInstance() {
        return INSTANCE;
    }

    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    public Activity getTopActivity() {
        if (activities.isEmpty()) {
            return null;
        }
        return activities.get(activities.size() - 1);
    }

    public void finishTopActivity() {
        if (!activities.isEmpty()) {
            activities.remove(activities.size() - 1).finish();
        }
    }

    public void finishActivity(Activity activity) {
        if (activity != null) {
            activities.remove(activity);
            activity.finish();
        }
    }

    public void finishActivity(Class activityClass) {
        for (Activity activity : activities) {
            if (activity.getClass().equals(activityClass)) {
                finishActivity(activity);
            }
        }
    }

    public void finishAllActivity() {
        if (!activities.isEmpty()) {
            Iterator<Activity> it_b = (Iterator<Activity>) activities.iterator();
            while(it_b.hasNext()) {
                Activity ac = it_b.next();
                ac.finish();
                it_b.remove();
            }
            for (Activity activity : activities) {
                activity.finish();
                activities.remove(activity);
            }
        }
    }
}

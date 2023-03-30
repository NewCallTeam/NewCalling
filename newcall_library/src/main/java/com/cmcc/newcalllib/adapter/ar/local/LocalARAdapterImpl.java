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

import android.content.Context;
import android.content.Intent;
import android.view.Surface;

public class LocalARAdapterImpl implements LocalARAdapter {
    private Context mContext;
//    private ARUtil mARUtil;

    public LocalARAdapterImpl(Context context) {
        mContext = context;
//        mARUtil = new ARUtil(mContext);
    }

    /**
     * Check local AR SDK Ability.
     * @return
     */
    public boolean checkARAbility() {
//        return mARUtil.checkARAbility();
        return true;
    }

    /**
     * Start AR Call interface, the ARFragment must be init in caller module.
     * @param surface
     * @param callback
     */
    public void startARAbility(Surface surface, LocalARCallback callback) {
//        Intent intent = new Intent(mContext, LocalARCallActivity.class);
//        intent.putExtra("native_surface", surface);
//        mContext.startActivity(intent);
//        callback.onStartARCallback(1000);
    }


    /**
     * stop AR Call, that will release the surface and set fragment gone(releaseARFragment).
     * @param callback
     */
    public void stopARAbility(LocalARCallback callback) {
//        ARActivityUtil.getInstance().finishAllActivity();
//        callback.onStopARCallback(1000);
    }
}

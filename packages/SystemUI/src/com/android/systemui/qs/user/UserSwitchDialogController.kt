/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.user

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.VisibleForTesting
import com.android.systemui.R
import com.android.systemui.animation.DialogLaunchAnimator
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.qs.tiles.UserDetailView
import com.android.systemui.statusbar.phone.SystemUIDialog
import javax.inject.Inject
import javax.inject.Provider

/**
 * Controller for [UserDialog].
 */
@SysUISingleton
class UserSwitchDialogController @VisibleForTesting constructor(
    private val userDetailViewAdapterProvider: Provider<UserDetailView.Adapter>,
    private val activityStarter: ActivityStarter,
    private val falsingManager: FalsingManager,
    private val dialogLaunchAnimator: DialogLaunchAnimator,
    private val dialogFactory: (Context) -> SystemUIDialog
) {

    @Inject
    constructor(
        userDetailViewAdapterProvider: Provider<UserDetailView.Adapter>,
        activityStarter: ActivityStarter,
        falsingManager: FalsingManager,
        dialogLaunchAnimator: DialogLaunchAnimator
    ) : this(
        userDetailViewAdapterProvider,
        activityStarter,
        falsingManager,
        dialogLaunchAnimator,
        { SystemUIDialog(it) }
    )

    companion object {
        private val USER_SETTINGS_INTENT = Intent(Settings.ACTION_USER_SETTINGS)
    }

    /**
     * Show a [UserDialog].
     *
     * Populate the dialog with information from and adapter obtained from
     * [userDetailViewAdapterProvider] and show it as launched from [view].
     */
    fun showDialog(view: View) {
        with(dialogFactory(view.context)) {
            setShowForAllUsers(true)
            setCanceledOnTouchOutside(true)

            setTitle(R.string.qs_user_switch_dialog_title)
            setPositiveButton(R.string.quick_settings_done, null)
            setNeutralButton(R.string.quick_settings_more_user_settings) { _, _ ->
                if (!falsingManager.isFalseTap(FalsingManager.LOW_PENALTY)) {
                    dialogLaunchAnimator.disableAllCurrentDialogsExitAnimations()
                    activityStarter.postStartActivityDismissingKeyguard(
                        USER_SETTINGS_INTENT,
                        0
                    )
                }
            }
            val gridFrame = LayoutInflater.from(this.context)
                .inflate(R.layout.qs_user_dialog_content, null)
            setView(gridFrame)

            val adapter = userDetailViewAdapterProvider.get()

            adapter.linkToViewGroup(gridFrame.findViewById(R.id.grid))

            val hostDialog = dialogLaunchAnimator.showFromView(this, view)
            adapter.injectDialogShower(DialogShowerImpl(hostDialog, dialogLaunchAnimator))
        }
    }

    private class DialogShowerImpl(
        private val hostDialog: Dialog,
        private val dialogLaunchAnimator: DialogLaunchAnimator
    ) : DialogInterface by hostDialog, DialogShower {
        override fun showDialog(dialog: Dialog): Dialog {
            return dialogLaunchAnimator.showFromDialog(
                dialog,
                parentHostDialog = hostDialog
            )
        }
    }

    interface DialogShower : DialogInterface {
        fun showDialog(dialog: Dialog): Dialog
    }
}
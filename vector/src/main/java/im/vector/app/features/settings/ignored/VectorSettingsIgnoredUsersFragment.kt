/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.ignored

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding

import javax.inject.Inject

class VectorSettingsIgnoredUsersFragment @Inject constructor(
        val ignoredUsersViewModelFactory: IgnoredUsersViewModel.Factory,
        private val ignoredUsersController: IgnoredUsersController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        IgnoredUsersController.Callback {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: IgnoredUsersViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.waitingView.waitingStatusText.setText(R.string.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
        ignoredUsersController.callback = this
        views.genericRecyclerView.configureWith(ignoredUsersController)
        viewModel.observeViewEvents {
            when (it) {
                is IgnoredUsersViewEvents.Loading -> showLoading(it.message)
                is IgnoredUsersViewEvents.Failure -> showFailure(it.throwable)
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        ignoredUsersController.callback = null
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.settings_ignored_users)
    }

    override fun onUserIdClicked(userId: String) {
        AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.settings_unignore_user, userId))
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.handle(IgnoredUsersAction.UnIgnore(userId))
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    // ==============================================================================================================
    // ignored users list management
    // ==============================================================================================================

    override fun invalidate() = withState(viewModel) { state ->
        ignoredUsersController.update(state)

        handleUnIgnoreRequestStatus(state.unIgnoreRequest)
    }

    private fun handleUnIgnoreRequestStatus(unIgnoreRequest: Async<Unit>) {
        views.waitingView.root.isVisible = when (unIgnoreRequest) {
            is Loading -> true
            else       -> false
        }
    }
}

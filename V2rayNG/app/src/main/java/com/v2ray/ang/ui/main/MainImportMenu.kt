package com.v2ray.ang.ui.main

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R

@Composable
fun ImportMenuContent(
    onAction: (MainAction) -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.menu_item_import_config_qrcode)) },
        onClick = { onAction(MainAction.ImportQRcode) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.menu_item_import_config_clipboard)) },
        onClick = { onAction(MainAction.ImportClipboard) }
    )
}

/** Deliberately carries no delete entries: configs cannot be removed from the main screen. */
@Composable
fun MoreMenuContent(
    onAction: (MainAction) -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_service_restart)) },
        onClick = { onAction(MainAction.RestartService) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_export_all)) },
        onClick = { onAction(MainAction.ExportAll) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_locate_selected_config)) },
        onClick = { onAction(MainAction.LocateSelectedServer) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_sort_by_test_results)) },
        onClick = { onAction(MainAction.SortByTestResults) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_real_ping_all_server)) },
        onClick = { onAction(MainAction.TestRealAllServers) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_sub_update)) },
        onClick = { onAction(MainAction.UpdateSubscriptions) }
    )
}

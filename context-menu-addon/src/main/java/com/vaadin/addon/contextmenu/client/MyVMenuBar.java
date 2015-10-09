package com.vaadin.addon.contextmenu.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.ui.VMenuBar;

/**
 * This is just to overcome the issue of application connection. Not needed
 * later, after this issue is resolved in the framework.
 */
public class MyVMenuBar extends VMenuBar {

	// FIXME: this should be properly set for all context menus
	private boolean isContextMenu = true;

	public MyVMenuBar() {
	}

	public MyVMenuBar(boolean subMenu, VMenuBar parentMenu) {
		super(subMenu, parentMenu);
	}

	// FIXME copy-paste from parent just to change VOverlay to MyVOverlay and
	// make this method visible to ContextMenuConnector
	protected void showChildMenuAt(CustomMenuItem item, int top, int left) {
		final int shadowSpace = 10;

		// FIXME only this line in changed
		popup = new MyVOverlay(true, false, true);
		popup.setOwner(this);

		/*
		 * Use parents primary style name if possible and remove the submenu
		 * prefix if needed
		 */
		String primaryStyleName = parentMenu != null ? parentMenu
				.getStylePrimaryName() : getStylePrimaryName();
		if (subMenu) {
			primaryStyleName = primaryStyleName.replace(
					SUBMENU_CLASSNAME_PREFIX, "");
		}
		popup.setStyleName(primaryStyleName + "-popup");

		// Setting owner and handlers to support tooltips. Needed for tooltip
		// handling of overlay widgets (will direct queries to parent menu)
		if (parentMenu == null) {
			popup.setOwner(this);
		} else {
			VMenuBar parent = parentMenu;
			while (parent.getParentMenu() != null) {
				parent = parent.getParentMenu();
			}
			popup.setOwner(parent);
		}
		if (client != null) {
			client.getVTooltip().connectHandlersToWidget(popup);
		}

		popup.setWidget(item.getSubMenu());
		popup.addCloseHandler(this);
		popup.addAutoHidePartner(item.getElement());

		// at 0,0 because otherwise IE7 add extra scrollbars (#5547)
		popup.setPopupPosition(0, 0);

		item.getSubMenu().onShow();
		visibleChildMenu = item.getSubMenu();
		item.getSubMenu().setParentMenu(this);

		popup.show();

		if (left + popup.getOffsetWidth() >= RootPanel.getBodyElement()
				.getOffsetWidth() - shadowSpace) {
			if (subMenu) {
				left = item.getParentMenu().getAbsoluteLeft()
						- popup.getOffsetWidth() - shadowSpace;
			} else {
				left = RootPanel.getBodyElement().getOffsetWidth()
						- popup.getOffsetWidth() - shadowSpace;
			}
			// Accommodate space for shadow
			if (left < shadowSpace) {
				left = shadowSpace;
			}
		}

		// top = adjustPopupHeight(top, shadowSpace);

		popup.setPopupPosition(left, top);

	}

	// this method has a couple lines added, marked with FIXME
	public boolean handleNavigation(int keycode, boolean ctrl, boolean shift) {

		// If tab or shift+tab close menus
		if (keycode == KeyCodes.KEY_TAB) {
			setSelected(null);
			hideChildren();
			menuVisible = false;
			return false;
		}

		if (ctrl || shift || !enabled) {
			// Do not handle tab key, nor ctrl keys
			return false;
		}

		if (keycode == getNavigationLeftKey()) {
			if (getSelected() == null) {
				// If nothing is selected then select the last item
				setSelected(items.get(items.size() - 1));
				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			} else if (visibleChildMenu == null && getParentMenu() == null) {
				// If this is the root menu then move to the left
				int idx = items.indexOf(getSelected());
				if (idx > 0) {
					setSelected(items.get(idx - 1));
				} else {
					setSelected(items.get(items.size() - 1));
				}

				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			} else if (visibleChildMenu != null) {
				// Redirect all navigation to the submenu
				visibleChildMenu.handleNavigation(keycode, ctrl, shift);

			} else if (getParentMenu().getParentMenu() == null) {

				// FIXME: this line added
				if (isContextMenu)
					return true;

				// Inside a sub menu, whose parent is a root menu item
				VMenuBar root = getParentMenu();

				root.getSelected().getSubMenu().setSelected(null);
				// #15255 - disable animate-in/out when hide popup
				root.hideChildren(false, false);

				// Get the root menus items and select the previous one
				int idx = root.getItems().indexOf(root.getSelected());
				idx = idx > 0 ? idx : root.getItems().size();
				CustomMenuItem selected = root.getItems().get(--idx);

				while (selected.isSeparator() || !selected.isEnabled()) {
					idx = idx > 0 ? idx : root.getItems().size();
					selected = root.getItems().get(--idx);
				}

				root.setSelected(selected);
				openMenuAndFocusFirstIfPossible(selected);
			} else {
				getParentMenu().getSelected().getSubMenu().setSelected(null);
				getParentMenu().hideChildren();
			}

			return true;

		} else if (keycode == getNavigationRightKey()) {

			if (getSelected() == null) {
				// If nothing is selected then select the first item
				setSelected(items.get(0));
				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			} else if (visibleChildMenu == null && getParentMenu() == null) {
				// If this is the root menu then move to the right
				int idx = items.indexOf(getSelected());

				if (idx < items.size() - 1) {
					setSelected(items.get(idx + 1));
				} else {
					setSelected(items.get(0));
				}

				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			} else if (visibleChildMenu == null
					&& getSelected().getSubMenu() != null) {
				// If the item has a submenu then show it and move the selection
				// there
				showChildMenu(getSelected());
				menuVisible = true;
				visibleChildMenu.handleNavigation(keycode, ctrl, shift);
			} else if (visibleChildMenu == null && !isContextMenu /* FIXME */) {

				// Get the root menu
				VMenuBar root = getParentMenu();
				while (root.getParentMenu() != null) {
					root = root.getParentMenu();
				}

				// Hide the submenu (#15255 - disable animate-in/out when hide
				// popup)
				root.hideChildren(false, false);

				// Get the root menus items and select the next one
				int idx = root.getItems().indexOf(root.getSelected());
				idx = idx < root.getItems().size() - 1 ? idx : -1;
				CustomMenuItem selected = root.getItems().get(++idx);

				while (selected.isSeparator() || !selected.isEnabled()) {
					idx = idx < root.getItems().size() - 1 ? idx : -1;
					selected = root.getItems().get(++idx);
				}

				root.setSelected(selected);
				openMenuAndFocusFirstIfPossible(selected);
			} else if (visibleChildMenu != null) {
				// Redirect all navigation to the submenu
				visibleChildMenu.handleNavigation(keycode, ctrl, shift);
			}

			return true;

		} else if (keycode == getNavigationUpKey()) {

			if (getSelected() == null) {
				// If nothing is selected then select the last item
				setSelected(items.get(items.size() - 1));
				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			} else if (visibleChildMenu != null) {
				// Redirect all navigation to the submenu
				visibleChildMenu.handleNavigation(keycode, ctrl, shift);
			} else {
				// Select the previous item if possible or loop to the last item
				int idx = items.indexOf(getSelected());
				if (idx > 0) {
					setSelected(items.get(idx - 1));
				} else {
					setSelected(items.get(items.size() - 1));
				}

				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			}

			return true;

		} else if (keycode == getNavigationDownKey()) {

			if (getSelected() == null) {
				// If nothing is selected then select the first item
				selectFirstItem();
			} else if (visibleChildMenu == null && getParentMenu() == null) {
				// If this is the root menu the show the child menu with arrow
				// down, if there is a child menu
				openMenuAndFocusFirstIfPossible(getSelected());
			} else if (visibleChildMenu != null) {
				// Redirect all navigation to the submenu
				visibleChildMenu.handleNavigation(keycode, ctrl, shift);
			} else {
				// Select the next item if possible or loop to the first item
				int idx = items.indexOf(getSelected());
				if (idx < items.size() - 1) {
					setSelected(items.get(idx + 1));
				} else {
					setSelected(items.get(0));
				}

				if (!getSelected().isSelectable()) {
					handleNavigation(keycode, ctrl, shift);
				}
			}
			return true;

		} else if (keycode == getCloseMenuKey()) {
			setSelected(null);
			hideChildren();
			menuVisible = false;

		} else if (isNavigationSelectKey(keycode)) {
			if (getSelected() == null) {
				// If nothing is selected then select the first item
				selectFirstItem();
			} else if (visibleChildMenu != null) {
				// Redirect all navigation to the submenu
				visibleChildMenu.handleNavigation(keycode, ctrl, shift);
				menuVisible = false;
			} else if (visibleChildMenu == null
					&& getSelected().getSubMenu() != null) {
				// If the item has a sub menu then show it and move the
				// selection there
				openMenuAndFocusFirstIfPossible(getSelected());
			} else {
				final Command command = getSelected().getCommand();

				setSelected(null);
				hideParents(true);

				// #17076 keyboard selected menuitem without children: do
				// not leave menu to visible ("hover open") mode
				menuVisible = false;

				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						if (command != null) {
							command.execute();
						}
					}
				});
			}
		}

		return false;
	}

	private void selectFirstItem() {
		for (int i = 0; i < items.size(); i++) {
			CustomMenuItem item = items.get(i);
			if (item.isSelectable()) {
				setSelected(item);
				break;
			}
		}
	}

	private void openMenuAndFocusFirstIfPossible(CustomMenuItem menuItem) {
		MyVMenuBar subMenu = (MyVMenuBar) menuItem.getSubMenu();
		if (subMenu == null) {
			// No child menu? Nothing to do
			return;
		}

		MyVMenuBar parentMenu = (MyVMenuBar) menuItem.getParentMenu();
		parentMenu.showChildMenu(menuItem);

		menuVisible = true;
		// Select the first item in the newly open submenu
		subMenu.selectFirstItem();

	}

	public boolean isPopupShowing() {
		return menuVisible;
	}	
}

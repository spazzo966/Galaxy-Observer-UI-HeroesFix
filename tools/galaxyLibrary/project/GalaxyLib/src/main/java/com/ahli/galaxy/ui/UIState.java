// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.galaxy.ui;

import com.ahli.galaxy.ui.abstracts.UIElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ahli
 */
public class UIState extends UIElement {
	
	private List<UIAttribute> whens;
	private List<UIAttribute> actions;
	private boolean nextAdditionShouldOverrideWhens;
	private boolean nextAdditionShouldOverrideActions;
	
	public UIState() {
		super(null);
		whens = new ArrayList<>(0);
		actions = new ArrayList<>(0);
	}
	
	/**
	 * @param name
	 */
	public UIState(final String name) {
		super(name);
		whens = new ArrayList<>(0);
		actions = new ArrayList<>(0);
	}
	
	/**
	 * @param name
	 * @param initialWhensCapacity
	 * @param initialActionsCapacity
	 */
	public UIState(final String name, final int initialWhensCapacity, final int initialActionsCapacity) {
		super(name);
		whens = new ArrayList<>(initialWhensCapacity);
		actions = new ArrayList<>(initialActionsCapacity);
	}
	
	/**
	 * Returns a deep clone of this.
	 */
	@Override
	public Object deepCopy() {
		final UIState clone = new UIState(getName(), whens.size(), actions.size());
		final List<UIAttribute> whensClone = clone.whens;
		for (UIAttribute when : whens) {
			whensClone.add((UIAttribute) when.deepCopy());
		}
		final List<UIAttribute> actionsClone = clone.actions;
		for (UIAttribute action : actions) {
			actionsClone.add((UIAttribute) action.deepCopy());
		}
		clone.nextAdditionShouldOverrideActions = nextAdditionShouldOverrideActions;
		clone.nextAdditionShouldOverrideWhens = nextAdditionShouldOverrideWhens;
		return clone;
	}
	
	/**
	 * @return the whens
	 */
	public List<UIAttribute> getWhens() {
		return whens;
	}
	
	/**
	 * @param whens
	 * 		the whens to set
	 */
	public void setWhens(final List<UIAttribute> whens) {
		this.whens = whens;
	}
	
	/**
	 * @return the actions
	 */
	public List<UIAttribute> getActions() {
		return actions;
	}
	
	/**
	 * @param actions
	 * 		the actions to set
	 */
	public void setActions(final List<UIAttribute> actions) {
		this.actions = actions;
	}
	
	/**
	 * @return the nextAdditionShouldOverrideWhens
	 */
	public boolean isNextAdditionShouldOverrideWhens() {
		return nextAdditionShouldOverrideWhens;
	}
	
	/**
	 * @param nextAdditionShouldOverrideWhens
	 * 		the nextAdditionShouldOverrideWhens to set
	 */
	public void setNextAdditionShouldOverrideWhens(final boolean nextAdditionShouldOverrideWhens) {
		this.nextAdditionShouldOverrideWhens = nextAdditionShouldOverrideWhens;
	}
	
	/**
	 * @return the nextAdditionShouldOverrideActions
	 */
	public boolean isNextAdditionShouldOverrideActions() {
		return nextAdditionShouldOverrideActions;
	}
	
	/**
	 * @param nextAdditionShouldOverrideActions
	 * 		the nextAdditionShouldOverrideActions to set
	 */
	public void setNextAdditionShouldOverrideActions(final boolean nextAdditionShouldOverrideActions) {
		this.nextAdditionShouldOverrideActions = nextAdditionShouldOverrideActions;
	}
	
	/**
	 * @param path
	 * @return
	 */
	@Override
	public UIElement receiveFrameFromPath(final String path) {
		return (path == null || path.isEmpty()) ? this : null;
	}
	
	@Override
	public String toString() {
		return "<State name='" + getName() + "'>";
	}
	
	@Override
	public List<UIElement> getChildren() {
		return Collections.emptyList();
	}
	
	@Override
	public List<UIElement> getChildrenRaw() {
		return null; // returning null is desired here
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		final Object[] signatureFields = getSignatureFields();
		final Object[] thatSignatureFields = ((UIState) obj).getSignatureFields();
		for (int i = 0; i < signatureFields.length; ++i) {
			if (!(signatureFields[i] instanceof Object[])) {
				if (!Objects.equals(signatureFields[i], thatSignatureFields[i])) {
					return false;
				}
			} else {
				if (!Arrays.deepEquals((Object[]) signatureFields[i], (Object[]) thatSignatureFields[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
	private Object[] getSignatureFields() {
		return new Object[] { getName(), whens, actions, nextAdditionShouldOverrideActions,
				nextAdditionShouldOverrideWhens };
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getSignatureFields());
	}
}

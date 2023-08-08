/**
 * @copyright actri.avic
 */
package avic.actri.findfile.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 */
public class FileListLabelProvider extends WorkbenchLabelProvider implements
		ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return super.getImage(element);
		} else {
			return null;
		}
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return super.getText(element);
		} else if (columnIndex == 1) {
			return ((IFile) element).getParent().getFullPath().toOSString()
					.substring(1);
		} else {
			return null;
		}
	}

	@Override
	public Color getForeground(Object element) {
		if (element instanceof IFile
				&& ((IFile) element).getName().startsWith(".")) {
			return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
		} else {
			return super.getForeground(element);
		}
	}
}

/**
 * @copyright actri.avic
 */
package avic.actri.findfile.ui;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

/**
 */
public class FindFileView extends ViewPart {

	private IEditorPart openInEditor(IFile file) throws CoreException {
		if (file == null)
			return null;
		if (!file.getProject().isAccessible()) {
			MessageBox errorMsg = new MessageBox(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(), SWT.ICON_ERROR
					| SWT.OK);
			errorMsg.setText("错误");
			errorMsg.setMessage(MessageFormat.format("项目关闭",
					new Object[] { file.getProject().getName() }));
			errorMsg.open();

			return null;
		}

		if (!file.isLinked(IResource.CHECK_ANCESTORS)) {
			File tempFile = file.getRawLocation().toFile();
			if (tempFile != null) {
				String canonicalPath = null;
				try {
					canonicalPath = tempFile.getCanonicalPath();
				} catch (IOException e) {
				}

				if (canonicalPath != null) {
					IPath path = new Path(canonicalPath);
					file = ResourcesPlugin.getWorkspace().getRoot()
							.getFileForLocation(path);
				}
			}
		}
		String editorID = file.getPersistentProperty(IDE.EDITOR_KEY);
		IEditorRegistry registry = PlatformUI.getWorkbench()
				.getEditorRegistry();
		IEditorDescriptor desc = registry.findEditor(editorID);
		if (desc == null) {
			editorID = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
		}
		IWorkbenchPage p = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage();
		return p.openEditor(new FileEditorInput(file), editorID, true);
	}

	private final class DoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent event) {
			Object obj = ((StructuredSelection) event.getSelection())
					.getFirstElement();
			if (obj instanceof IFile) {
				try {
					openInEditor((IFile) obj);
				} catch (CoreException e) {
				}
			}
		}
	}

	private class NamePatternFilter extends ViewerFilter {
		private String regex;
		private StringMatcher matcher;
		private String[] srcTypes;

		public NamePatternFilter() {
			regex = "*"; //$NON-NLS-1$
			matcher = new StringMatcher(regex, true, false);
			srcTypes = new String[] { ".c", ".cc", ".c++", ".cpp", ".asm",
					".h", ".cxx", ".s", ".hh" };
		}

		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if ("".equals(regex)) { //$NON-NLS-1$
				return true;
			}

			IFile file = (IFile) element;
			String filename = file.getName();

			if (fAction_onlyShowSrc.isChecked()) {
				boolean matchFileType = false;
				for (String fileType : srcTypes) {
					if (filename.toLowerCase().endsWith(fileType)) {
						matchFileType = true;
						break;
					}
				}

				if (!matchFileType) {
					return false;
				}
			}

			return matcher.match(filename);
		}

		public void setRegex(String regex) {
			this.regex = regex;
			StringBuffer sb = new StringBuffer();
			sb.append("*"); //$NON-NLS-1$
			sb.append(regex);
			sb.append("*"); //$NON-NLS-1$
			matcher.setPattern(sb.toString());
		}

		public void ignoreCase(boolean ignoreCase) {
			matcher.ignoreCases(ignoreCase);
		}
	}

	private class NameSorter extends ViewerSorter {
		private int fSortType = 0;

		public int compare(Viewer viewer, Object e1, Object e2) {
			String s1 = ((IFile) e1).getName();
			String s2 = ((IFile) e2).getName();
			switch (fSortType) {
			case 0:
			case 1:
				s1 = ((IFile) e1).getName();
				s2 = ((IFile) e2).getName();
				break;
			case 2:
			case 3:
				s1 = ((IFile) e1).getParent().getFullPath().toOSString();
				s2 = ((IFile) e2).getParent().getFullPath().toOSString();
				break;
			default:
				break;
			}

			switch (fSortType) {
			case 0:
			case 2:
				return s1.compareTo(s2);
			case 1:
			case 3:
				return s2.compareTo(s1);
			default:
				return super.compare(viewer, s1, s2);
			}
		}
	}

	private Combo fText_input;

	private NamePatternFilter fNameFilter;
	private NameSorter fNameSorter;

	private TableViewer fViewer;
	private String fFilter;

	private Action fAction_onlyShowSrc;
	private Action fAction_IgnoreCase;

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	protected void createDialogArea(Composite parent) {
		Label label = createMessageArea(parent);
		label.setText("查找文件");
		createFilterText(parent);
		createFilteredList(parent);
	}

	/**
	 * Creates a filtered list.
	 * 
	 * @param parent
	 *            the parent composite.
	 * @return returns the filtered list widget.
	 */
	protected void createFilteredList(Composite parent) {
		fViewer = new TableViewer(parent, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		fViewer.setColumnProperties(new String[] { "0,1" }); //$NON-NLS-1$
		Table tableApp = fViewer.getTable();
		tableApp.setHeaderVisible(true);
		tableApp.setLinesVisible(false);
		tableApp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2,
				1));

		TableViewerColumn tableViewerColumn = new TableViewerColumn(fViewer,
				SWT.NONE);
		TableColumn tableColumn = tableViewerColumn.getColumn();
		tableColumn.setWidth(80);
		tableColumn.setText("文件");
		tableColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fNameSorter.fSortType == 0) {
					fNameSorter.fSortType = 1;
				} else if (fNameSorter.fSortType == 1) {
					fNameSorter.fSortType = 0;
				} else {
					fNameSorter.fSortType = 0;
				}
				fViewer.refresh();
			}
		});

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(fViewer,
				SWT.NONE);
		TableColumn tableColumn_1 = tableViewerColumn_1.getColumn();
		tableColumn_1.setWidth(200);
		tableColumn_1.setText("路径");
		tableColumn_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fNameSorter.fSortType == 2) {
					fNameSorter.fSortType = 3;
				} else if (fNameSorter.fSortType == 3) {
					fNameSorter.fSortType = 2;
				} else {
					fNameSorter.fSortType = 2;
				}
				fViewer.refresh();
			}
		});

		fNameSorter = new NameSorter();
		fViewer.setLabelProvider(new FileListLabelProvider());
		fViewer.setContentProvider(new FileListProvider(fViewer));
		fViewer.setUseHashlookup(true);
		fViewer.setSorter(fNameSorter);
		fNameFilter = new NamePatternFilter();
		fViewer.addFilter(fNameFilter);

		fViewer.setInput(ResourcesPlugin.getWorkspace());
		fViewer.addDoubleClickListener(new DoubleClickListener());

		getSite().setSelectionProvider(fViewer);
	}

	protected void createFilterText(Composite parent) {
		Combo text = new Combo(parent, SWT.BORDER);
		fText_input = text;

		text.setText((fFilter == null ? "" : fFilter)); //$NON-NLS-1$
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				final String regex = fText_input.getText().trim();
				BusyIndicator.showWhile(fViewer.getControl().getDisplay(),
						new Runnable() {
							public void run() {
								fNameFilter.setRegex(regex);
								fViewer.refresh(false);
							}
						});
			}
		});

		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR) {
					fViewer.getControl().setFocus();
					addItemToCombo(fText_input, fText_input.getText().trim());
				}
			}
		});

	}

	private void addItemToCombo(Combo combo, String item) {
		String[] items = combo.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(item)) {
				return;
			}
		}
		combo.add(item, 0);
	}

	/**
	 * Creates the message text widget and sets layout data.
	 * 
	 * @param composite
	 *            the parent composite of the message area.
	 */
	protected Label createMessageArea(Composite composite) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(""); //$NON-NLS-1$
		label.setFont(composite.getFont());

		GridData data = new GridData();
		data.grabExcessVerticalSpace = false;
		data.grabExcessHorizontalSpace = false;
		data.horizontalAlignment = GridData.CENTER;
		data.verticalAlignment = GridData.BEGINNING;
		label.setLayoutData(data);

		return label;
	}

	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		makeActions();
		createDialogArea(parent);
		hookContextMenu();
		contributeToActionBars();
	}

	public void dispose() {
		super.dispose();
	}

	private void fillContextMenu(IMenuManager manager) {
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(fAction_onlyShowSrc);
		manager.add(fAction_IgnoreCase);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fViewer);
	}

	private void makeActions() {
		fAction_onlyShowSrc = new Action("只显示源代码文件", Action.AS_CHECK_BOX) {
			public void run() {
				fViewer.refresh(false);
			}
		};
		fAction_onlyShowSrc.setChecked(false);

		fAction_IgnoreCase = new Action("忽略大小写", Action.AS_CHECK_BOX) {
			public void run() {
				fNameFilter.ignoreCase(!isChecked());
				fViewer.refresh(false);
			}
		};
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		fText_input.setFocus();
	}
}
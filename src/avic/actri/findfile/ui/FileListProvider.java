/**
 * @copyright actri.avic
 */
package avic.actri.findfile.ui;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

/**
 */
public class FileListProvider implements ITreeContentProvider,
		IResourceChangeListener {
	private Viewer fViewer;
	private boolean updateFlag = false;

	public FileListProvider(Viewer viewer) {
		fViewer = viewer;
	}

	public void resourceChanged(IResourceChangeEvent event) {
		final IResourceDelta delta = event.getDelta();
		if (delta != null) {
			try {
				delta.accept(new IResourceDeltaVisitor() {
					public final boolean visit(final IResourceDelta current)
							throws CoreException {
						final IResource resource = current.getResource();
						if (!resource.isDerived()) {
							if (resource.getType() == IResource.FILE) {
								switch (delta.getKind()) {
								case IResourceDelta.ADDED:
									fAllFiles.add((IFile) resource);
									updateFlag = true;
									break;
								case IResourceDelta.REMOVED:
									fAllFiles.remove((IFile) resource);
									updateFlag = true;
									break;
								case IResourceDelta.CHANGED:
									if (current.getKind() == IResourceDelta.ADDED) {
										fAllFiles.add((IFile) resource);
										updateFlag = true;
									} else if (current.getKind() == IResourceDelta.REMOVED) {
										fAllFiles.remove((IFile) resource);
										updateFlag = true;
									}
									break;
								}
							}
						}
						return true;
					}
				});
			} catch (CoreException exception) {
			}
		}
		if (updateFlag) {
			updateFlag = false;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					fViewer.refresh();
				}
			});
		}
	}

	private List<IFile> fAllFiles;

	private void addContainerToList(IContainer container) {
		try {
			for (IResource res : container.members()) {
				if (res instanceof IContainer) {
					addContainerToList((IContainer) res);
				} else if (res instanceof IFile) {
					fAllFiles.add(((IFile) res));
				}
			}
		} catch (CoreException e) {
			// Ignore Exceptions, cause it does not matter
		}
	}

	@Override
	public void dispose() {
		if (fAllFiles != null) {
			fAllFiles.clear();
		}
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (fAllFiles == null) {
			fAllFiles = new LinkedList<IFile>();
			updataFileList();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
					IResourceChangeEvent.POST_CHANGE);
		}
		return fAllFiles.toArray();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IWorkspace;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	private void updataFileList() {
		fAllFiles.clear();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			addContainerToList(project);
		}
	}
}

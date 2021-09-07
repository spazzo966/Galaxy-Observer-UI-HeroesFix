// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.build;

import com.ahli.galaxy.game.GameData;
import interfacebuilder.base_ui.BaseUiService;
import interfacebuilder.projects.Project;
import interfacebuilder.threads.CleaningForkJoinTask;
import interfacebuilder.threads.CleaningForkJoinTaskCleaner;

import java.io.Serial;

public class BuildTask extends CleaningForkJoinTask {
	
	@Serial
	private static final long serialVersionUID = 1114165634840947017L;
	
	private final transient Project project;
	private final boolean useCmdLineSettings;
	private final transient MpqBuilderService mpqBuilderService;
	private final transient BaseUiService baseUiService;
	
	public BuildTask(
			final CleaningForkJoinTaskCleaner cleaner,
			final Project project,
			final boolean useCmdLineSettings,
			final MpqBuilderService mpqBuilderService,
			final BaseUiService baseUiService) {
		super(cleaner);
		this.project = project;
		this.useCmdLineSettings = useCmdLineSettings;
		this.mpqBuilderService = mpqBuilderService;
		this.baseUiService = baseUiService;
	}
	
	@Override
	protected boolean work() {
		final GameData gameData = mpqBuilderService.getGameData(project.getGame());
		baseUiService.parseBaseUiIfNecessary(gameData, useCmdLineSettings);
		
		mpqBuilderService.buildSpecificUI(gameData, useCmdLineSettings, project);
		
		return true;
	}
}

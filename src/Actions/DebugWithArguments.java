package Actions;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import javax.swing.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This action-plugin debugs a program with program parameters
 **/
public class DebugWithArguments extends AnAction {


    /**
     * This method is called by intellij multiple times per second to keep the plugin appearance updated
     * It controls whether the Run With Arguments button is showing or not, as well as what the displayed name of the button is
     *
     * @param e contains useful information about the nature of the action performed
     */
    @Override
    public void update(AnActionEvent e){
        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE); //Extracts the current file from the context
        boolean show = true;
        if(file == null){
            e.getPresentation().setVisible(false); //If its not a file being selected then it automatically doesn't show
            return;
        }
        else {
            try {
                show = file.getFileType().getName().equals("JAVA"); //The menu bar only opens if you are selecting a .java file
            } catch(Exception exc){
                e.getPresentation().setVisible(false);
                return;
            }
            PsiJavaFile jfile;
            if(show){
                jfile = (PsiJavaFile) file; //If it is a java file then cast it into a jfile
                show = RunWithArguments.hasMainMethod(jfile); //We only show the menu option if the program contains a main method
            }
        }
        e.getPresentation().setVisible(show); //Sets the visibility of the button
        if(show)e.getPresentation().setText("Debug \'" + file.getName().substring(0 , file.getName().lastIndexOf('.')) + "\' with Arguments"); //Sets the display as the name of the file
    }
    /**
     * This method is called every time the button provided on the IDE is clicked
     *
     * @param e contains useful information about the nature of the action performed
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project proj = e.getProject(); //Gets the project stored before any UI actions
        if(proj == null)return;
        String filename; //The name of the class that you are running. If the class is named "HelloWorld.java" filename will store "HelloWorld"
        Module mod; //The module that the class is in
        PsiJavaFile file; //The psijavafile representation of the class file being ran
        RunnerAndConfigurationSettings config; //the configuration settings for the run manager
        ApplicationConfiguration apconfig; //stores the application specific run settings
        try{
            file = (PsiJavaFile) e.getData(PlatformDataKeys.PSI_FILE); //Gets the java file from the Action Event details
        } catch(Exception exc){
            return;
        }
        if(file == null)return; //Kicks out if there is no file selected
        try {
            mod = LangDataKeys.MODULE.getData(e.getDataContext()); //Gets the module from the data context of the Action Event information
        } catch(Exception exc){
            return;
        }
        if(mod == null)return;
        filename = file.getPackageName(); //Initially sets the filename as the package name
        filename += filename.length() == 0 ? "" : "."; //Adds a period to the filename if the program is in a package
        try {
            filename += (file.getName().substring(0, file.getName().lastIndexOf('.'))); //Adds the actual program file name to the filename string. After all these lines the filename is correctly set to "package.class" or "class"
        } catch(Exception exc) {
            return;
        }
        RunManager rm = RunManager.getInstance(proj); //Extracts the run manager from the project
        String orig = ""; //This variable stores the original value displayed in the program argument,  initially it is equal to the empty string
        RunnerAndConfigurationSettings prev = null;
        for(RunnerAndConfigurationSettings rcs : rm.getConfigurationSettingsList(ApplicationConfigurationType.getInstance())){
            ApplicationConfiguration ac;
            try {
                ac = (ApplicationConfiguration) rcs.getConfiguration(); //FIX THIS
            } catch(Exception exc){
                continue; //If it's not an application configuration then continue
            }
            if(ac.getMainClassName() != null && ac.getMainClassName().equals(filename) && ac.getModules()[0].getName().equals(mod.getName())){ //Checks to see if this file had a previous configuration with program parameters
                orig = ac.getProgramParameters(); //Sets the original text to the previous program parameters
                prev = rcs; //Gets the previous configuration
            }
        }
        String a = (String) JOptionPane.showInputDialog(null, "Enter Program Arguments: ", "Run \'" + filename + "\' with Arguments", 3, null, null, orig);
        if (a == null)
            return; //If the user x's out or cancels we don't add the program to the run manager or run it
        if(prev == null) {
            apconfig = new ApplicationConfiguration(filename, proj , ApplicationConfigurationType.getInstance()); //Instantiates a new Application Configuration instance using the filename as the name
            config = rm.createConfiguration(apconfig , apconfig.getFactory());
            config.setName(filename); //Sets the name of the configuration as the file name
            apconfig = (ApplicationConfiguration) config.getConfiguration();
            apconfig.setMainClassName(filename); //Sets the filename of the application configuration
            apconfig.setWorkingDirectory(proj.getBaseDir().getPath()); //Sets the working directory to the directory of the project
            apconfig.setProgramParameters(a); //Prompts the user to enter program parameters
            apconfig.setModuleName(mod.getName()); //Sets the module name in the application configuration
            apconfig.getBeforeRunTasks().clear();
            prev = rm.getConfigurationTemplate(apconfig.getFactory()); //Next few lines set the settings to the default settings
            config.setTemporary(prev.isTemporary());
            config.setActivateToolWindowBeforeRun(prev.isActivateToolWindowBeforeRun());
            config.setShared(prev.isShared());
            config.setSingleton(prev.isSingleton());
            config.setEditBeforeRun(prev.isEditBeforeRun());
            rm.addConfiguration(config);
            rm.setSelectedConfiguration(config); //sets the configuration as the current selected configuration
            ProgramRunnerUtil.executeConfiguration(config, DefaultDebugExecutor.getDebugExecutorInstance()); //Debugs the application
        }
        else{
            config = prev; //Uses the previous configuration settings
            apconfig = (ApplicationConfiguration) config.getConfiguration(); //Extracts the application specific settings out of the settings
            apconfig.setProgramParameters(a); //Prompts the user to enter program parameters
            rm.setSelectedConfiguration(config);
            ProgramRunnerUtil.executeConfiguration(config, DefaultDebugExecutor.getDebugExecutorInstance()); //Debugs the application
        }
    }
}

/**
 * To Do:
 * 1) Documentation
 * 4) Settings xml file
 * 5) Make the name of the command change depending on the file
 *
 **/

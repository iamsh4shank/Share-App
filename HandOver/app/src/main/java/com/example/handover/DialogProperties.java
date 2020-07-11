package com.example.handover;

import com.github.angads25.filepicker.model.DialogConfigs;

import java.io.File;

public class DialogProperties extends com.github.angads25.filepicker.model.DialogProperties {

    public int selection_mode;

    /** Selection Type defines that whether a File/Directory or both of these has
     *  to be selected.
     *
     *  FILE_SELECT ,DIR_SELECT, FILE_AND_DIR_SELECT are the three selection modes,
     *  See DialogConfigs for more info. Set to FILE_SELECT as default value by constructor.
     */
    public int selection_type;

    /**  The Parent/Root Directory. List of Files are populated from here. Can be set
     *  to any readable directory. /sdcard is the default location.
     *
     *  Eg. /sdcard
     *  Eg. /mnt
     */
    public File root;

    /**  The Directory is used when Root Directory is not readable or accessible. /
     *  sdcard is the default location.
     *
     *  Eg. /sdcard
     *  Eg. /mnt
     */
    public File error_dir;

    /** The Directory can be used as an offset. It is the first directory that is
     *  shown in dialog. Consider making it Root's sub-directory.
     *
     *  Eg. Root: /sdcard
     *  Eg. Offset: /sdcard/Music/Country
     *
     */
    public File offset;

    /** An Array of String containing extensions, Files with only that will be shown.
     *  Others will be ignored. Set to null as default value by constructor.
     *  Eg. String ext={"jpg","jpeg","png","gif"};
     */
    public String[] extensions;

    public DialogProperties() {
        selection_mode = DialogConfigs.MULTI_MODE;
        selection_type = DialogConfigs.FILE_SELECT;
        root = new File(DialogConfigs.DEFAULT_DIR);
        error_dir = new File(DialogConfigs.DEFAULT_DIR);
        offset = new File(DialogConfigs.DEFAULT_DIR);
        extensions = null;
    }

}

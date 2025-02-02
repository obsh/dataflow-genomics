package com.google.allenday.genomics.core.align.transform;

import com.google.allenday.genomics.core.align.SamBamManipulationService;
import com.google.allenday.genomics.core.gene.GeneData;
import com.google.allenday.genomics.core.gene.GeneExampleMetaData;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SortFn extends DoFn<KV<GeneExampleMetaData, GeneData>, KV<GeneExampleMetaData, GeneData>> {

    private Logger LOG = LoggerFactory.getLogger(SortFn.class);
    private GCSService gcsService;

    private TransformIoHandler transformIoHandler;
    private FileUtils fileUtils;
    private SamBamManipulationService samBamManipulationService;

    public SortFn(TransformIoHandler transformIoHandler, FileUtils fileUtils, SamBamManipulationService samBamManipulationService) {
        this.transformIoHandler = transformIoHandler;
        this.fileUtils = fileUtils;
        this.samBamManipulationService = samBamManipulationService;
    }

    @Setup
    public void setUp() {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        LOG.info(String.format("Start of sort with input: %s", c.element().toString()));

        KV<GeneExampleMetaData, GeneData> input = c.element();
        GeneData geneData = input.getValue();
        GeneExampleMetaData geneExampleMetaData = input.getKey();

        if (geneExampleMetaData == null || geneData == null) {
            LOG.error("Data error");
            LOG.error("geneExampleMetaData: " + geneExampleMetaData);
            LOG.error("geneData: " + geneData);
            return;
        }
        try {
            String workingDir = fileUtils.makeDirByCurrentTimestampAndSuffix(geneExampleMetaData.getRunId());
            try {
                String inputFilePath = transformIoHandler.handleInputAsLocalFile(gcsService, geneData, workingDir);
                String alignedSortedBamPath = samBamManipulationService.sortSam(
                        inputFilePath, workingDir, geneExampleMetaData.getRunId(), geneData.getReferenceName());

                c.output(KV.of(geneExampleMetaData, transformIoHandler.handleFileOutput(gcsService, alignedSortedBamPath, geneData.getReferenceName())));
            } catch (IOException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            } finally {
                fileUtils.deleteDir(workingDir);
            }
        } catch (RuntimeException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

    }
}

import sys, os, time, subprocess, random
import threading
from multiprocessing import Process
import matplotlib.pyplot as plt
import palettable
import numpy as np
import pandas as pd
import csv

colors = palettable.colorbrewer.qualitative.Paired_10.hex_colors
linestyles = ['-', '--', ":"]
# plt.rc('font',**{'size': 16, 'family': 'Arial' })
plt.rc('pdf',fonttype = 42)

def main():
    fig, ax = plt.subplots(figsize=(7, 4))
    with open('./plot.csv') as file:
        reader = csv.reader(file)
        count=0
        for row in reader:
            if count==0:
                fct_dctcp_mean=[]
                fct_dctcp_median=[]
                fct_dctcp_99=[]
                for i in range(16,23):
                    fct_dctcp_mean.append(float(row[i]))
                for i in range(23,30):
                    fct_dctcp_median.append(float(row[i]))
                for i in range(30,37):
                    fct_dctcp_99.append(float(row[i]))
            elif count==1:
                fct_pifo_mean=[]
                fct_pifo_median=[]
                fct_pifo_99=[]
                for i in range(16,23):
                    fct_pifo_mean.append(float(row[i]))
                for i in range(23,30):
                    fct_pifo_median.append(float(row[i]))
                for i in range(30,37):
                    fct_pifo_99.append(float(row[i]))
            elif count==2:
                fct_aifo_mean=[]
                fct_aifo_median=[]
                fct_aifo_99=[]
                for i in range(16,23):
                    fct_aifo_mean.append(float(row[i]))
                for i in range(23,30):
                    fct_aifo_median.append(float(row[i]))
                for i in range(30,37):
                    fct_aifo_99.append(float(row[i]))
            elif count==3:
                fct_sqwfq_mean=[]
                fct_sqwfq_median=[]
                fct_sqwfq_99=[]
                for i in range(16,23):
                    fct_sqwfq_mean.append(float(row[i]))
                for i in range(23,30):
                    fct_sqwfq_median.append(float(row[i]))
                for i in range(30,37):
                    fct_sqwfq_99.append(float(row[i]))
            elif count==4:
                fct_sqswfq_mean=[]
                fct_sqswfq_median=[]
                fct_sqswfq_99=[]
                for i in range(16,23):
                    fct_sqswfq_mean.append(float(row[i]))
                for i in range(23,30):
                    fct_sqswfq_median.append(float(row[i]))
                for i in range(30,37):
                    fct_sqswfq_99.append(float(row[i]))
            elif count==5:
                fct_pcq_mean=[]
                fct_pcq_median=[]
                fct_pcq_99=[]
                for i in range(16,23):
                    fct_pcq_mean.append(float(row[i]))
                for i in range(23,30):
                    fct_pcq_median.append(float(row[i]))
                for i in range(30,37):
                    fct_pcq_99.append(float(row[i]))
            count += 1

    _mean_funcs = [fct_dctcp_mean,fct_pifo_mean,fct_aifo_mean,fct_sqwfq_mean,fct_sqswfq_mean,fct_pcq_mean]
    _median_funcs = [fct_dctcp_median,fct_pifo_median,fct_aifo_median,fct_sqwfq_median,fct_sqswfq_median,fct_pcq_median]
    _99th_funcs = [fct_dctcp_99,fct_pifo_99,fct_aifo_99,fct_sqwfq_99,fct_sqswfq_99,fct_pcq_99]

    for x in range(len(_mean_funcs)):
        for i in range(len(_99th_funcs[x])):
            _99th_funcs[x][i] = _99th_funcs[x][i] - _mean_funcs[x][i]

    width = 0.1
    idx = [[0.1, 0.8, 1.5, 2.2, 2.9, 3.6, 4.3],
        [0.2, 0.9, 1.6, 2.3, 3.0, 3.7, 4.4],
        [0.3, 1.0, 1.7, 2.4, 3.1, 3.8, 4.5],
        [0.4, 1.1, 1.8, 2.5, 3.2, 3.9, 4.6],
        [0.5, 1.2, 1.9, 2.6, 3.3, 4.0, 4.7],
        [0.6, 1.3, 2.0, 2.7, 3.4, 4.1, 4.8]]

    xticks_idx = [0.35, 1.05, 1.75, 2.45, 3.15, 3.85, 4.55]
    # xticks_idx = idx[2]
    xticks = ["10K", "20K", "30K", "50K", "80K", "0.2M-1M", "$\geq$2M"]
    plt.xticks(xticks_idx, xticks)
    low = [0, 0, 0, 0, 0, 0, 0]
    ax.bar(idx[0], _mean_funcs[0], yerr=(low, _99th_funcs[0]), error_kw=dict(lw=1, capsize=3, capthick=1), width=width, align='center', ecolor=colors[6], capsize=10, label = "DCTCP", color=colors[6])
    ax.bar(idx[1], _mean_funcs[2], yerr=(low, _99th_funcs[2]), error_kw=dict(lw=1, capsize=3, capthick=1), width=width, align='center', ecolor=colors[9], capsize=10, label = "AIFO", color=colors[9])
    ax.bar(idx[2], _mean_funcs[5], yerr=(low, _99th_funcs[5]), error_kw=dict(lw=1, capsize=3, capthick=1), width=width, align='center', ecolor=colors[5], capsize=10, label = "PCQ", color=colors[5])
    ax.bar(idx[3], _mean_funcs[3], yerr=(low, _99th_funcs[3]), error_kw=dict(lw=1, capsize=3, capthick=1), width=width, align='center', ecolor=colors[4], capsize=10, label = "SQWFQ", color=colors[4])
    ax.bar(idx[4], _mean_funcs[4], yerr=(low, _99th_funcs[4]), error_kw=dict(lw=1, capsize=3, capthick=1), width=width, align='center', ecolor=colors[3], capsize=10, label = "SQSWFQ", color=colors[3])
    ax.bar(idx[5], _mean_funcs[1], yerr=(low, _99th_funcs[1]), error_kw=dict(lw=1, capsize=3, capthick=1), width=width, align='center', ecolor=colors[1], capsize=10, label = "PIFO", color=colors[1])

    l = plt.legend(numpoints=1, prop={'size':14}, loc='upper center', bbox_to_anchor=(0.5, 1.25), ncol=3)
    l.set_frame_on(False)

    ax.set_yscale('log')
    ax.set_xlabel('Flow size')
    ax.xaxis.set_ticks_position('bottom')
    ax.set_ylabel('Flow completion time (ms)')
    ax.yaxis.set_ticks_position('left')

    plt.savefig("allflow.pdf", bbox_inches='tight')
    return

if __name__ == '__main__':
    main()
'''
compute graph-based importance features
'''
import os, codecs
import networkx as nx
import util
import pandas as pd

folder = 'data/CMapSummaries/dummy'
name = 'concept-graph'

for topic in os.listdir(folder):

    # load graph
    g_mult_dir = nx.read_gml(os.path.join(folder, topic, name+'.graph'))
    g_mult_undir = g_mult_dir.to_undirected();
    g_single_dir = nx.Graph(g_mult_dir)
    g_single_undir = g_single_dir.to_undirected();
    print topic, len(g_mult_dir)
    
    data = {}
    data['_id'] = { n: str(n) for n in g_mult_dir.nodes() }
        
    # A) centrality measures
    print 'centrality'
    data['in_degree'] = g_mult_dir.in_degree()
    data['out_degree'] = g_mult_dir.out_degree()
    data['degree_centrality'] = { n: d/float(g_mult_undir.size()) for n,d in g_mult_undir.degree().items() }
    data['closeness_centrality'] = nx.closeness_centrality(g_mult_undir)
    data['betweenness_centrality'] = nx.betweenness_centrality(g_mult_undir)
    data['eigenvector_centrality'] = nx.eigenvector_centrality_numpy(g_single_undir, weight=None)
    data['katz_centrality'] = nx.katz_centrality_numpy(g_single_undir, weight=None)
    
    # B) link analysis
    print 'link analysis'
    data['page_rank'] = nx.pagerank_numpy(g_mult_undir, weight=None)
    hits = nx.hits_numpy(g_mult_dir)
    data['hits_hub'] = hits[0]
    data['hits_auth'] = hits[1]
    
    # C) Reichherzer & Leake 2006
    print 'cmaps'
    data['u_weights'] = util.comp_u_weights(g_mult_dir)
    data['l_weights'] = util.comp_l_weights(g_mult_dir)
    # HARD 
    p = [0, 2.235, 1.764]
    data['hard'] = { n: p[0]*data['hits_hub'][n] + p[1]*data['hits_auth'][n] + p[2]*data['u_weights'][n] for n in g_mult_dir.nodes() }
    # CRD
    p = [2.22, 3, 1.8125]
    degrees = g_mult_undir.degree()
    root = max(g_mult_undir.nodes(), key=lambda n: degrees[n])
    data['root_dist'] = nx.shortest_path_length(g_mult_undir, source=root)
    data['root_dist'][root] = 0
    for n in [x for x in g_mult_undir.nodes() if x not in data['root_dist']]:
        data['root_dist'][n] = len(g_mult_undir)
    data['crd'] = { n: p[0]*data['out_degree'][n] + p[1]*data['in_degree'][n] + (1/(data['root_dist'][n]+1))**p[2] for n in g_mult_dir.nodes() }
    
    # D) graph degeneracy - Tixier et al. 2006
    print 'degeneracy'
    data['core_number'] = nx.core_number(g_single_undir)
    data['core_rank'] = { n: sum([data['core_number'][nb] for nb in g_single_undir.neighbors(n)]) for n in g_single_undir.nodes() }
    
    # write to file
    d = []
    cols = sorted(list(data.keys()))
    with codecs.open(os.path.join(folder, topic, name+'.graph_features.tsv'), 'w', encoding='utf-8') as f:
        f.write('\t'.join(cols) + '\n')
        for n in g_mult_dir.nodes():
            f.write('\t'.join([str(data[c][n]) for c in cols]) + '\n')
            d.append([data[c][n] for c in cols])
            
    #df = pd.DataFrame(d, columns=cols)
    #print df.describe()
    #print df.corr()
    

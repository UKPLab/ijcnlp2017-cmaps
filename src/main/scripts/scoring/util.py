
import numpy as np


def comp_u_weights(g, max_iter=100, tol=1e-06):
    ''' upper-weight for nodes in graph (see Canas et al. 2001) '''
    return comp_lu_weights(g, g.predecessors, max_iter, tol)
    
def comp_l_weights(g, max_iter=100, tol=1e-06):
    ''' lower-weight for nodes in graph (see Canas et al. 2001) '''
    return comp_lu_weights(g, g.successors, max_iter, tol)

def comp_lu_weights(g, nb_func, max_iter=100, tol=1e-06):
    
    nodes = list(g.nodes())
    n2i = { n:i for i,n in enumerate(nodes) }
    u = np.ones(len(g))
    
    for i in range(0,max_iter):
    
        # 1) update
        u_new = np.copy(u)
        for n in range(0,len(g)):
            nbs = nb_func(nodes[n])
            if len(nbs) == 0:
                u_new[n] = 1
            else:
                u_new[n] = sum([u[n2i[x]]**2 for x in nbs])
                
        # 2) normalization
        u_new = u_new / np.sqrt(np.square(u_new).sum())
        
        # 3) check for convergence
        if np.all(np.abs(u_new-u) < tol):
            u = u_new
            break
        else:      
            u = u_new
            
    if i == max_iter:
        print 'lu_weights did not converge'
            
    return { n: u[n2i[n]] for n in nodes }